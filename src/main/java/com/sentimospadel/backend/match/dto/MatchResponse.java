package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchStatus;
import java.time.Instant;
import java.util.List;

public record MatchResponse(
        Long id,
        Long createdByPlayerProfileId,
        MatchStatus status,
        Instant scheduledAt,
        Long clubId,
        String locationText,
        String notes,
        Integer maxPlayers,
        Integer currentPlayerCount,
        boolean resultExists,
        MatchResultSummaryResponse result,
        List<MatchParticipantResponse> participants,
        Instant createdAt,
        Instant updatedAt
) {
}
