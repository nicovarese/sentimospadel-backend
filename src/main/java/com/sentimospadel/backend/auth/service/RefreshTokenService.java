package com.sentimospadel.backend.auth.service;

import com.sentimospadel.backend.auth.config.RefreshTokenProperties;
import com.sentimospadel.backend.auth.entity.AuthRefreshToken;
import com.sentimospadel.backend.auth.repository.AuthRefreshTokenRepository;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;

    @Transactional
    public IssuedRefreshToken issue(User user) {
        String rawToken = generateRefreshToken();
        AuthRefreshToken savedToken = refreshTokenRepository.save(AuthRefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plus(refreshTokenProperties.getExpiration()))
                .build());

        return new IssuedRefreshToken(rawToken, savedToken.getExpiresAt());
    }

    @Transactional
    public User rotateAndResolveUser(String rawRefreshToken) {
        AuthRefreshToken refreshToken = requireActiveToken(rawRefreshToken);
        refreshToken.setRevokedAt(Instant.now());
        return refreshToken.getUser();
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        AuthRefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).orElse(null);
        if (refreshToken == null || refreshToken.getRevokedAt() != null) {
            return;
        }
        refreshToken.setRevokedAt(Instant.now());
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeActiveTokensForUser(userId, Instant.now());
    }

    private AuthRefreshToken requireActiveToken(String rawRefreshToken) {
        AuthRefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BadRequestException("Refresh token invalido."));

        Instant now = Instant.now();
        if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(now)) {
            throw new BadRequestException("Refresh token invalido.");
        }

        User user = refreshToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("La cuenta no esta activa.");
        }

        return refreshToken;
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
    }
}
