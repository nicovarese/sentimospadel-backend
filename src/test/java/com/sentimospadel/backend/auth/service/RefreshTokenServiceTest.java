package com.sentimospadel.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.auth.config.RefreshTokenProperties;
import com.sentimospadel.backend.auth.entity.AuthRefreshToken;
import com.sentimospadel.backend.auth.repository.AuthRefreshTokenRepository;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private AuthRefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setExpiration(Duration.ofDays(30));
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, properties);
    }

    @Test
    void issueStoresOnlyHashedRefreshTokenAndReturnsRawTokenOnce() {
        User user = activeUser();
        when(refreshTokenRepository.save(any(AuthRefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(user);

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void rotateRejectsRevokedToken() {
        AuthRefreshToken token = AuthRefreshToken.builder()
                .user(activeUser())
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .revokedAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.rotateAndResolveUser("raw-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Refresh token invalido.");
    }

    private User activeUser() {
        return User.builder()
                .id(10L)
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hash")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
