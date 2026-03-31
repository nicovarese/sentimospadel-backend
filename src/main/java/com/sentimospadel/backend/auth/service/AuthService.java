package com.sentimospadel.backend.auth.service;

import com.sentimospadel.backend.auth.dto.CurrentUserResponse;
import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.LoginResponse;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.RegisterResponse;
import com.sentimospadel.backend.auth.enums.RegisterAccountType;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.DuplicateResourceException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("A user with email " + normalizedEmail + " already exists");
        }

        User user = buildUserForRegistration(normalizedEmail, request);

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + normalizedEmail + " was not found"));

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

    private User buildUserForRegistration(String normalizedEmail, RegisterRequest request) {
        if (request.accountType() == RegisterAccountType.CLUB) {
            Club managedClub = clubRepository.save(Club.builder()
                    .name(requireNonBlank(request.clubName(), "Club name is required"))
                    .city(requireNonBlank(request.clubCity(), "Club city is required"))
                    .address(trimToNull(request.clubAddress()))
                    .integrated(true)
                    .build());

            return User.builder()
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .managedClub(managedClub)
                    .build();
        }

        return User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
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
        return email.trim().toLowerCase();
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
