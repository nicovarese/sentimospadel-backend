package com.sentimospadel.backend.match.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.match-invitations")
public record MatchInvitationProperties(
        String baseUrl,
        Duration expiration
) {
}
