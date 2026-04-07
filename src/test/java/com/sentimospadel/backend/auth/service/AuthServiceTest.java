package com.sentimospadel.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.auth.dto.CurrentUserResponse;
import com.sentimospadel.backend.auth.dto.EmailVerificationDispatchResponse;
import com.sentimospadel.backend.auth.dto.EmailVerificationPageResult;
import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.LoginResponse;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.RegisterResponse;
import com.sentimospadel.backend.auth.dto.ResendEmailVerificationRequest;
import com.sentimospadel.backend.auth.enums.RegisterAccountType;
import com.sentimospadel.backend.auth.config.EmailVerificationProperties;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.legal.service.LegalDocumentService;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.DuplicateResourceException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private final EmailVerificationProperties emailVerificationProperties = new EmailVerificationProperties();

    @Mock
    private EmailVerificationNotificationService emailVerificationNotificationService;

    @Mock
    private LegalDocumentService legalDocumentService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                clubRepository,
                playerProfileRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                emailVerificationProperties,
                emailVerificationNotificationService,
                legalDocumentService
        );
    }

    @Test
    void registerHashesPasswordPersistsPhoneAndKeepsUserPendingVerification() {
        RegisterRequest request = new RegisterRequest(
                " Player Uno ",
                " Player@Example.com ",
                " 091 234 567 ",
                "secret123",
                RegisterAccountType.PLAYER,
                null,
                null,
                null,
                true,
                "2026-04-07.1",
                true,
                "2026-04-07.1",
                true,
                true,
                "2026-04-07.1"
        );

        when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("091234567")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setCreatedAt(Instant.parse("2026-03-13T12:00:00Z"));
            user.setUpdatedAt(Instant.parse("2026-03-13T12:00:00Z"));
            return user;
        });
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("player@example.com", savedUser.getEmail());
        assertEquals("091234567", savedUser.getPhone());
        assertTrue(passwordEncoder.matches("secret123", savedUser.getPasswordHash()));
        assertEquals(UserRole.PLAYER, savedUser.getRole());
        assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, savedUser.getStatus());
        assertTrue(savedUser.getEmailVerificationTokenHash() != null && !savedUser.getEmailVerificationTokenHash().isBlank());
        assertEquals("player@example.com", response.email());
        verify(playerProfileRepository).save(any(PlayerProfile.class));
        verify(emailVerificationNotificationService).sendVerificationEmail(eq("player@example.com"), eq("Player Uno"), any());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Player", "player@example.com", "091234567", "secret123", RegisterAccountType.PLAYER, null, null, null, true, "2026-04-07.1", true, "2026-04-07.1", false, false, "2026-04-07.1");

        when(userRepository.existsByEmail("player@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void registerRejectsDuplicatePhone() {
        RegisterRequest request = new RegisterRequest("Player", "player@example.com", "091 234 567", "secret123", RegisterAccountType.PLAYER, null, null, null, true, "2026-04-07.1", true, "2026-04-07.1", false, false, "2026-04-07.1");

        when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("091234567")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void loginReturnsUserDataForValidCredentials() {
        User user = User.builder()
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setCreatedAt(Instant.parse("2026-03-13T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-03-13T12:00:00Z"));

        LoginRequest request = new LoginRequest(" Player@Example.com ", "secret123");

        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(userRepository.findByEmail("player@example.com")).thenReturn(java.util.Optional.of(user));

        LoginResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any());
        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("player@example.com", response.email());
        assertEquals(UserRole.PLAYER, response.role());
        assertEquals(UserStatus.ACTIVE, response.status());
    }

    @Test
    void registerClubAccountCreatesManagedClubAdmin() {
        RegisterRequest request = new RegisterRequest(
                null,
                " club@example.com ",
                " 098 765 432 ",
                "secret123",
                RegisterAccountType.CLUB,
                " Club de Prueba ",
                " Montevideo ",
                " Rivera 1234 ",
                true,
                "2026-04-07.1",
                true,
                "2026-04-07.1",
                false,
                true,
                "2026-04-07.1"
        );

        when(userRepository.existsByEmail("club@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("098765432")).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setCreatedAt(Instant.parse("2026-03-30T12:00:00Z"));
            user.setUpdatedAt(Instant.parse("2026-03-30T12:00:00Z"));
            return user;
        });

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("club@example.com", savedUser.getEmail());
        assertEquals("098765432", savedUser.getPhone());
        assertEquals(UserRole.ADMIN, savedUser.getRole());
        assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, savedUser.getStatus());
        assertEquals("Club de Prueba", savedUser.getManagedClub().getName());
        assertEquals("Montevideo", savedUser.getManagedClub().getCity());
        assertEquals("Club de Prueba", response.managedClubName());
        verify(emailVerificationNotificationService).sendVerificationEmail(eq("club@example.com"), eq("Club de Prueba"), any());
    }

    @Test
    void loginRejectsPendingEmailVerification() {
        User user = User.builder()
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(java.util.Optional.of(user));

        assertThrows(BadRequestException.class, () -> authService.login(new LoginRequest("player@example.com", "secret123")));
    }

    @Test
    void loginRejectsInvalidCredentials() {
        LoginRequest request = new LoginRequest("player@example.com", "wrongpass");
        User user = User.builder()
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(java.util.Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void getCurrentUserReturnsSafeUserData() {
        User user = User.builder()
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(java.util.Optional.of(user));

        CurrentUserResponse response = authService.getCurrentUser("player@example.com");

        assertEquals("player@example.com", response.email());
        assertEquals(UserRole.PLAYER, response.role());
        assertEquals(UserStatus.ACTIVE, response.status());
    }

    @Test
    void resendEmailVerificationRefreshesTokenForPendingUser() {
        User user = User.builder()
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.PENDING_EMAIL_VERIFICATION)
                .emailVerificationTokenHash("old-token")
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(java.util.Optional.of(user));

        EmailVerificationDispatchResponse response = authService.resendEmailVerification(
                new ResendEmailVerificationRequest("player@example.com")
        );

        assertTrue(response.message().contains("Te reenviamos"));
        assertFalse("old-token".equals(user.getEmailVerificationTokenHash()));
        verify(emailVerificationNotificationService).sendVerificationEmail(eq("player@example.com"), eq("Player"), any());
    }

    @Test
    void verifyEmailActivatesPendingUser() {
        RegisterRequest request = new RegisterRequest(
                "Player Uno",
                "player@example.com",
                "091234567",
                "secret123",
                RegisterAccountType.PLAYER,
                null,
                null,
                null,
                true,
                "2026-04-07.1",
                true,
                "2026-04-07.1",
                false,
                false,
                "2026-04-07.1"
        );

        when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("091234567")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailVerificationNotificationService).sendVerificationEmail(eq("player@example.com"), eq("Player Uno"), tokenCaptor.capture());

        User persistedUser = userCaptor.getValue();
        when(userRepository.findByEmailVerificationTokenHash(any())).thenReturn(java.util.Optional.of(persistedUser));

        EmailVerificationPageResult result = authService.verifyEmail(tokenCaptor.getValue());

        assertTrue(result.success());
        assertEquals(UserStatus.ACTIVE, persistedUser.getStatus());
        assertTrue(persistedUser.getEmailVerifiedAt() != null);
        assertNull(persistedUser.getEmailVerificationTokenHash());
    }
}
