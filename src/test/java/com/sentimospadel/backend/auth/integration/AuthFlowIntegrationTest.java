package com.sentimospadel.backend.auth.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import com.sentimospadel.backend.BackendApplication;
import com.sentimospadel.backend.auth.dto.EmailVerificationPageResult;
import com.sentimospadel.backend.auth.dto.CurrentUserResponse;
import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.LoginResponse;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.RegisterResponse;
import com.sentimospadel.backend.auth.dto.ResendEmailVerificationRequest;
import com.sentimospadel.backend.auth.enums.RegisterAccountType;
import com.sentimospadel.backend.auth.service.AuthService;
import com.sentimospadel.backend.auth.service.EmailVerificationNotificationService;
import com.sentimospadel.backend.player.enums.PreferredSide;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = BackendApplication.class)
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailVerificationNotificationService emailVerificationNotificationService;

    @Test
    void registerVerifyThenLoginThenMeWorksEndToEnd() {
        String normalizedEmail = "fresh.player@example.com";
        String password = "secret123";

        RegisterResponse registerResponse = authService.register(new RegisterRequest(
                "Fresh Player",
                " Fresh.Player@example.com ",
                "091234567",
                password,
                RegisterAccountType.PLAYER,
                null,
                null,
                null,
                "https://cdn.example.com/fresh-player.png",
                PreferredSide.RIGHT,
                "4.0",
                "Montevideo",
                null,
                true,
                "2026-04-07.1",
                true,
                "2026-04-07.1",
                true,
                true,
                "2026-04-07.1"
        ));

        assertEquals(normalizedEmail, registerResponse.email());
        assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, registerResponse.status());

        User persistedUser = userRepository.findByEmail(normalizedEmail).orElseThrow();
        assertEquals(normalizedEmail, persistedUser.getEmail());
        assertEquals("091234567", persistedUser.getPhone());
        assertTrue(passwordEncoder.matches(password, persistedUser.getPasswordHash()));
        assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, persistedUser.getStatus());

        assertThrows(BadRequestException.class, () -> authService.login(new LoginRequest(" Fresh.Player@example.com ", password)));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailVerificationNotificationService).sendVerificationEmail(anyString(), anyString(), tokenCaptor.capture());

        EmailVerificationPageResult verificationResult = authService.verifyEmail(tokenCaptor.getValue());
        assertTrue(verificationResult.success());

        LoginResponse loginResponse = authService.login(new LoginRequest(" Fresh.Player@example.com ", password));

        assertEquals(normalizedEmail, loginResponse.email());
        assertNotNull(loginResponse.accessToken());
        assertTrue(!loginResponse.accessToken().isBlank());

        CurrentUserResponse currentUserResponse = authService.getCurrentUser(loginResponse.email());
        assertEquals(normalizedEmail, currentUserResponse.email());
    }

    @Test
    void registerClubThenLoginThenMeWorksEndToEnd() {
        String normalizedEmail = "club.owner@example.com";
        String password = "secret123";

        RegisterResponse registerResponse = authService.register(new RegisterRequest(
                null,
                " Club.Owner@example.com ",
                "098765432",
                password,
                RegisterAccountType.CLUB,
                "Club Montevideo",
                "Montevideo",
                "Rivera 1234",
                null,
                null,
                null,
                null,
                null,
                true,
                "2026-04-07.1",
                true,
                "2026-04-07.1",
                false,
                true,
                "2026-04-07.1"
        ));

        assertEquals(normalizedEmail, registerResponse.email());
        assertEquals(UserRole.ADMIN, registerResponse.role());
        assertEquals("Club Montevideo", registerResponse.managedClubName());
        assertEquals(UserStatus.PENDING_EMAIL_VERIFICATION, registerResponse.status());

        User persistedUser = userRepository.findByEmail(normalizedEmail).orElseThrow();
        assertEquals(UserRole.ADMIN, persistedUser.getRole());
        assertNotNull(persistedUser.getManagedClub());
        assertEquals("Club Montevideo", persistedUser.getManagedClub().getName());
        assertEquals("098765432", persistedUser.getPhone());
        assertTrue(passwordEncoder.matches(password, persistedUser.getPasswordHash()));

        assertThrows(BadRequestException.class, () -> authService.login(new LoginRequest(" Club.Owner@example.com ", password)));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailVerificationNotificationService).sendVerificationEmail(anyString(), anyString(), tokenCaptor.capture());
        assertTrue(authService.verifyEmail(tokenCaptor.getValue()).success());

        LoginResponse loginResponse = authService.login(new LoginRequest(" Club.Owner@example.com ", password));

        assertEquals(normalizedEmail, loginResponse.email());
        assertEquals(UserRole.ADMIN, loginResponse.role());
        assertEquals("Club Montevideo", loginResponse.managedClubName());
        assertNotNull(loginResponse.accessToken());
        assertTrue(!loginResponse.accessToken().isBlank());

        CurrentUserResponse currentUserResponse = authService.getCurrentUser(loginResponse.email());
        assertEquals(normalizedEmail, currentUserResponse.email());
        assertEquals(UserRole.ADMIN, currentUserResponse.role());
        assertEquals("Club Montevideo", currentUserResponse.managedClubName());
    }

    @Test
    void resendEmailVerificationKeepsPendingAccountRecoverable() {
        authService.register(new RegisterRequest(
                "Recover Player",
                "recover.player@example.com",
                "091111222",
                "secret123",
                RegisterAccountType.PLAYER,
                null,
                null,
                null,
                null,
                PreferredSide.LEFT,
                "3.5",
                "Maldonado",
                null,
                true,
                "2026-04-07.1",
                true,
                "2026-04-07.1",
                false,
                false,
                "2026-04-07.1"
        ));

        var response = authService.resendEmailVerification(new ResendEmailVerificationRequest("recover.player@example.com"));

        assertTrue(response.message().contains("Te reenviamos"));
    }
}
