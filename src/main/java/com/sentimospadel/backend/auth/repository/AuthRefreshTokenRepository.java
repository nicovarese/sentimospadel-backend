package com.sentimospadel.backend.auth.repository;

import com.sentimospadel.backend.auth.entity.AuthRefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update AuthRefreshToken token
            set token.revokedAt = :revokedAt
            where token.user.id = :userId
              and token.revokedAt is null
            """)
    int revokeActiveTokensForUser(Long userId, Instant revokedAt);
}
