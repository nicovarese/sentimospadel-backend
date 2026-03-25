package com.sentimospadel.backend.auth.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sentimospadel.backend.BackendApplication;
import com.sentimospadel.backend.auth.dto.CurrentUserResponse;
import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.LoginResponse;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.RegisterResponse;
import com.sentimospadel.backend.auth.service.AuthService;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Test
    void registerThenLoginThenMeWorksEndToEnd() {
        String normalizedEmail = "fresh.player@example.com";
        String password = "secret123";

        RegisterResponse registerResponse = authService.register(new RegisterRequest(" Fresh.Player@example.com ", password));

        assertEquals(normalizedEmail, registerResponse.email());

        User persistedUser = userRepository.findByEmail(normalizedEmail).orElseThrow();
        assertEquals(normalizedEmail, persistedUser.getEmail());
        assertTrue(passwordEncoder.matches(password, persistedUser.getPasswordHash()));

        LoginResponse loginResponse = authService.login(new LoginRequest(" Fresh.Player@example.com ", password));

        assertEquals(normalizedEmail, loginResponse.email());
        assertNotNull(loginResponse.accessToken());
        assertTrue(!loginResponse.accessToken().isBlank());

        CurrentUserResponse currentUserResponse = authService.getCurrentUser(loginResponse.email());
        assertEquals(normalizedEmail, currentUserResponse.email());
    }
}
