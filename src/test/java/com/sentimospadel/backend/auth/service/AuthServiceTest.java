package com.sentimospadel.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.LoginResponse;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.RegisterResponse;
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
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager);
    }

    @Test
    void registerHashesPasswordAndPersistsUser() {
        RegisterRequest request = new RegisterRequest(" Player@Example.com ", "secret123");

        when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setCreatedAt(Instant.parse("2026-03-13T12:00:00Z"));
            user.setUpdatedAt(Instant.parse("2026-03-13T12:00:00Z"));
            return user;
        });

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("player@example.com", savedUser.getEmail());
        assertTrue(passwordEncoder.matches("secret123", savedUser.getPasswordHash()));
        assertEquals(UserRole.PLAYER, savedUser.getRole());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals("player@example.com", response.email());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("player@example.com", "secret123");

        when(userRepository.existsByEmail("player@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void loginReturnsUserDataForValidCredentials() {
        User user = User.builder()
                .email("player@example.com")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setCreatedAt(Instant.parse("2026-03-13T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-03-13T12:00:00Z"));

        LoginRequest request = new LoginRequest(" Player@Example.com ", "secret123");

        when(userRepository.findByEmail("player@example.com")).thenReturn(java.util.Optional.of(user));

        LoginResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any());
        assertEquals("player@example.com", response.email());
        assertEquals(UserRole.PLAYER, response.role());
        assertEquals(UserStatus.ACTIVE, response.status());
    }

    @Test
    void loginRejectsInvalidCredentials() {
        LoginRequest request = new LoginRequest("player@example.com", "wrongpass");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
