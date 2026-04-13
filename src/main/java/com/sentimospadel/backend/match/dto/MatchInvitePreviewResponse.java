package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchStatus;
import java.time.Instant;

public record MatchInvitePreviewResponse(
        Long matchId,
        MatchStatus status,
        Instant scheduledAt,
        Long clubId,
        String clubName,
        String courtName,
        String locationText,
        String createdByName,
        Integer currentPlayerCount,
        Integer maxPlayers,
        Instant expiresAt
) {
}
