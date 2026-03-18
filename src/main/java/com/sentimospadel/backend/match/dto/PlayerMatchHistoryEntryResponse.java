package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchStatus;
import java.time.Instant;
import java.util.List;

public record PlayerMatchHistoryEntryResponse(
        Long id,
        MatchStatus status,
        Instant scheduledAt,
        Long clubId,
        String locationText,
        String notes,
        Integer currentPlayerCount,
        List<MatchParticipantResponse> participants,
        boolean resultExists,
        MatchResultSummaryResponse result,
        boolean authenticatedPlayerIsParticipant,
        MatchParticipantTeam authenticatedPlayerTeam,
        Boolean authenticatedPlayerWon,
        Instant createdAt,
        Instant updatedAt
) {
}
