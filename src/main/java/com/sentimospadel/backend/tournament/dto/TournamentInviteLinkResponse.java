package com.sentimospadel.backend.tournament.dto;

import java.time.Instant;

public record TournamentInviteLinkResponse(
        Long tournamentId,
        String inviteToken,
        String inviteUrl,
        Instant expiresAt
) {
}
