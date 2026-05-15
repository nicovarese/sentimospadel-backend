package com.sentimospadel.backend.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String INSECURE_DEFAULT_FRAGMENT = "change-this-local-jwt-secret";

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.security.jwt.secret (JWT_SECRET) must be set");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.security.jwt.secret (JWT_SECRET) must be at least " + MIN_SECRET_LENGTH + " characters"
            );
        }
        if (secret.contains(INSECURE_DEFAULT_FRAGMENT)) {
            String activeProfiles = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
            if (activeProfiles != null && (activeProfiles.contains("staging") || activeProfiles.contains("production"))) {
                throw new IllegalStateException(
                        "app.security.jwt.secret (JWT_SECRET) is using the insecure local default in profile " + activeProfiles
                );
            }
        }
    }
}
