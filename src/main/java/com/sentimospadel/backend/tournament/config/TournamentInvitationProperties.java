package com.sentimospadel.backend.tournament.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tournament-invitations")
public record TournamentInvitationProperties(
        String baseUrl,
        Duration expiration
) {
}
