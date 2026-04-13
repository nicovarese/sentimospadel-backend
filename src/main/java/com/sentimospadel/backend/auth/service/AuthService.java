package com.sentimospadel.backend.auth.service;

import com.sentimospadel.backend.auth.config.EmailVerificationProperties;
import com.sentimospadel.backend.auth.dto.CurrentUserResponse;
import com.sentimospadel.backend.auth.dto.EmailVerificationDispatchResponse;
import com.sentimospadel.backend.auth.dto.EmailVerificationPageResult;
import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.LoginResponse;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.RegisterResponse;
import com.sentimospadel.backend.auth.dto.ResendEmailVerificationRequest;
import com.sentimospadel.backend.auth.enums.RegisterAccountType;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.enums.ClubBookingMode;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.legal.service.LegalDocumentService;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.PreferredSide;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.DuplicateResourceException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationProperties emailVerificationProperties;
    private final EmailVerificationNotificationService emailVerificationNotificationService;
    private final LegalDocumentService legalDocumentService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phone());
        Instant now = Instant.now();

        validateRegistrationConsents(request);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Ya existe una cuenta con el correo " + normalizedEmail);
        }

        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new DuplicateResourceException("Ya existe una cuenta con el telefono " + normalizedPhone);
        }

        String rawVerificationToken = generateVerificationToken();
        User user = buildUserForRegistration(normalizedEmail, normalizedPhone, request, rawVerificationToken, now);
        User savedUser = userRepository.save(user);

        if (request.accountType() == RegisterAccountType.PLAYER) {
            createInitialPlayerProfile(savedUser, request);
        }

        emailVerificationNotificationService.sendVerificationEmail(
                savedUser.getEmail(),
                registrationDisplayName(savedUser, request),
                rawVerificationToken
        );

        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + normalizedEmail + " was not found"));

        if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new BadRequestException("Debes confirmar tu email antes de iniciar sesion.");
        }

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password())
        );

        return new LoginResponse(
                jwtService.generateAccessToken(user),
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getManagedClub() != null ? user.getManagedClub().getId() : null,
                user.getManagedClub() != null ? user.getManagedClub().getName() : null
        );
    }

    @Transactional
    public EmailVerificationDispatchResponse resendEmailVerification(ResendEmailVerificationRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            return new EmailVerificationDispatchResponse("Si existe una cuenta pendiente para ese correo, reenviamos el link de confirmacion.");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            return new EmailVerificationDispatchResponse("Ese correo ya fue confirmado. Ya podes iniciar sesion.");
        }

        if (user.getStatus() != UserStatus.PENDING_EMAIL_VERIFICATION) {
            return new EmailVerificationDispatchResponse("No pudimos reenviar la verificacion para esta cuenta.");
        }

        String rawVerificationToken = generateVerificationToken();
        user.setEmailVerificationTokenHash(hashVerificationToken(rawVerificationToken));
        user.setEmailVerificationTokenExpiresAt(nextVerificationExpiry());

        emailVerificationNotificationService.sendVerificationEmail(
                user.getEmail(),
                resolveVerificationDisplayName(user),
                rawVerificationToken
        );

        return new EmailVerificationDispatchResponse("Te reenviamos el link de confirmacion a " + normalizedEmail + ".");
    }

    @Transactional
    public EmailVerificationPageResult verifyEmail(String rawToken) {
        String normalizedToken = requireNonBlank(rawToken, "Verification token is required");
        User user = userRepository.findByEmailVerificationTokenHash(hashVerificationToken(normalizedToken))
                .orElse(null);

        if (user == null) {
            return new EmailVerificationPageResult(
                    false,
                    "Link invalido",
                    "Este link de confirmacion no es valido o ya fue usado.",
                    "Volver al login",
                    emailVerificationProperties.getLoginUrl()
            );
        }

        Instant now = Instant.now();
        if (user.getEmailVerificationTokenExpiresAt() == null || user.getEmailVerificationTokenExpiresAt().isBefore(now)) {
            user.setEmailVerificationTokenHash(null);
            user.setEmailVerificationTokenExpiresAt(null);
            return new EmailVerificationPageResult(
                    false,
                    "Link vencido",
                    "El link de confirmacion vencio. Reenvia uno nuevo desde la pantalla de login.",
                    "Volver al login",
                    emailVerificationProperties.getLoginUrl()
            );
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerifiedAt(now);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiresAt(null);

        return new EmailVerificationPageResult(
                true,
                "Email confirmado",
                "Tu correo ya quedo confirmado. Ahora podes iniciar sesion en Sentimos Padel.",
                "Ir al login",
                emailVerificationProperties.getLoginUrl()
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " was not found"));

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getManagedClub() != null ? user.getManagedClub().getId() : null,
                user.getManagedClub() != null ? user.getManagedClub().getName() : null
        );
    }

    private User buildUserForRegistration(
            String normalizedEmail,
            String normalizedPhone,
            RegisterRequest request,
            String rawVerificationToken,
            Instant now
    ) {
        if (request.accountType() == RegisterAccountType.CLUB) {
            Club managedClub = clubRepository.save(Club.builder()
                    .name(requireNonBlank(request.clubName(), "Club name is required"))
                    .city(requireNonBlank(request.clubCity(), "Club city is required"))
                    .address(trimToNull(request.clubAddress()))
                    .integrated(true)
                    .bookingMode(ClubBookingMode.DIRECT)
                    .build());

            return User.builder()
                    .email(normalizedEmail)
                    .phone(normalizedPhone)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .role(UserRole.ADMIN)
                    .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                    .emailVerificationTokenHash(hashVerificationToken(rawVerificationToken))
                    .emailVerificationTokenExpiresAt(nextVerificationExpiry())
                    .acceptedTermsVersion(request.acceptedTermsVersion().trim())
                    .acceptedTermsAt(now)
                    .acceptedPrivacyVersion(request.acceptedPrivacyVersion().trim())
                    .acceptedPrivacyAt(now)
                    .consentPreferencesVersion(request.consentPreferencesVersion().trim())
                    .activityTrackingEnabled(Boolean.TRUE.equals(request.allowActivityTracking()))
                    .activityTrackingUpdatedAt(now)
                    .operationalNotificationsEnabled(Boolean.TRUE.equals(request.allowOperationalNotifications()))
                    .operationalNotificationsUpdatedAt(now)
                    .managedClub(managedClub)
                    .build();
        }

        return User.builder()
                .email(normalizedEmail)
                .phone(normalizedPhone)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.PLAYER)
                .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                .emailVerificationTokenHash(hashVerificationToken(rawVerificationToken))
                .emailVerificationTokenExpiresAt(nextVerificationExpiry())
                .acceptedTermsVersion(request.acceptedTermsVersion().trim())
                .acceptedTermsAt(now)
                .acceptedPrivacyVersion(request.acceptedPrivacyVersion().trim())
                .acceptedPrivacyAt(now)
                .consentPreferencesVersion(request.consentPreferencesVersion().trim())
                .activityTrackingEnabled(Boolean.TRUE.equals(request.allowActivityTracking()))
                .activityTrackingUpdatedAt(now)
                .operationalNotificationsEnabled(Boolean.TRUE.equals(request.allowOperationalNotifications()))
                .operationalNotificationsUpdatedAt(now)
                .build();
    }

    private void validateRegistrationConsents(RegisterRequest request) {
        if (!request.acceptTerms()) {
            throw new BadRequestException("Debes aceptar los Terminos y Condiciones para crear la cuenta.");
        }

        if (!request.acceptPrivacyPolicy()) {
            throw new BadRequestException("Debes aceptar la Politica de Privacidad para crear la cuenta.");
        }

        legalDocumentService.validateTermsVersion(request.acceptedTermsVersion());
        legalDocumentService.validatePrivacyVersion(request.acceptedPrivacyVersion());
        legalDocumentService.validateConsentPreferencesVersion(request.consentPreferencesVersion());
    }

    private void createInitialPlayerProfile(User user, RegisterRequest request) {
        playerProfileRepository.save(PlayerProfile.builder()
                .user(user)
                .fullName(requireNonBlank(request.fullName(), "Full name is required"))
                .photoUrl(trimToNull(request.photoUrl()))
                .preferredSide(requirePreferredSide(request.preferredSide()))
                .declaredLevel(requireNonBlank(request.declaredLevel(), "Declared level is required"))
                .city(requireNonBlank(request.city(), "City is required"))
                .representedClub(resolveRepresentedClub(request.representedClubId()))
                .currentRating(BigDecimal.valueOf(1.00).setScale(2))
                .provisional(true)
                .matchesPlayed(0)
                .ratedMatchesCount(0)
                .surveyCompleted(false)
                .requiresClubVerification(false)
                .clubVerificationStatus(ClubVerificationStatus.NOT_REQUIRED)
                .build());
    }

    private PreferredSide requirePreferredSide(PreferredSide preferredSide) {
        if (preferredSide == null) {
            throw new BadRequestException("Preferred side is required");
        }

        return preferredSide;
    }

    private Club resolveRepresentedClub(Long representedClubId) {
        if (representedClubId == null) {
            return null;
        }

        return clubRepository.findById(representedClubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club with id " + representedClubId + " was not found"));
    }

    private RegisterResponse toResponse(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getManagedClub() != null ? user.getManagedClub().getId() : null,
                user.getManagedClub() != null ? user.getManagedClub().getName() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        String trimmed = requireNonBlank(phone, "Phone is required");
        String normalized = trimmed
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".", "");

        if (normalized.startsWith("00")) {
            normalized = "+" + normalized.substring(2);
        }

        if (normalized.startsWith("+")) {
            String digits = normalized.substring(1).replaceAll("\\D", "");
            if (digits.length() < 7) {
                throw new BadRequestException("Phone must contain at least 7 digits");
            }
            return "+" + digits;
        }

        normalized = normalized.replaceAll("\\D", "");
        if (normalized.length() < 7) {
            throw new BadRequestException("Phone must contain at least 7 digits");
        }

        return normalized;
    }

    private String registrationDisplayName(User user, RegisterRequest request) {
        if (user.getManagedClub() != null) {
            return user.getManagedClub().getName();
        }

        return requireNonBlank(request.fullName(), "Full name is required");
    }

    private String resolveVerificationDisplayName(User user) {
        if (user.getManagedClub() != null) {
            return user.getManagedClub().getName();
        }

        if (user.getId() != null) {
            return playerProfileRepository.findByUserId(user.getId())
                    .map(PlayerProfile::getFullName)
                    .filter(fullName -> !fullName.isBlank())
                    .orElseGet(() -> deriveFullNameFromEmail(user.getEmail()));
        }

        return deriveFullNameFromEmail(user.getEmail());
    }

    private Instant nextVerificationExpiry() {
        return Instant.now().plus(emailVerificationProperties.getExpiration());
    }

    private String generateVerificationToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashVerificationToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String deriveFullNameFromEmail(String email) {
        String localPart = email == null ? "Jugador" : email.split("@", 2)[0];
        String normalized = localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();

        if (normalized.isBlank()) {
            return "Jugador";
        }

        String[] words = normalized.split("\\s+");
        StringBuilder fullName = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                fullName.append(' ');
            }

            String word = words[i];
            fullName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                fullName.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }

        return fullName.toString();
    }

    private String requireNonBlank(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BadRequestException(message);
        }

        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
