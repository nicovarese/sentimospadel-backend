package com.sentimospadel.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sentimospadel.backend.config.security.JwtProperties;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generateAndValidateToken() {
        JwtService jwtService = new JwtService(
                new JwtProperties("change-this-local-jwt-secret-change-this-local-jwt-secret", 3600000)
        );

        User user = User.builder()
                .email("player@example.com")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtService.generateAccessToken(user);

        assertEquals("player@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, "player@example.com"));
    }
}
