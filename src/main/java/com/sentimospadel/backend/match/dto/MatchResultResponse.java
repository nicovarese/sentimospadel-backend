package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import java.time.Instant;

public record MatchResultResponse(
        Long matchId,
        Long submittedByPlayerProfileId,
        MatchResultStatus status,
        MatchWinnerTeam winnerTeam,
        MatchScoreResponse score,
        Instant submittedAt,
        Long confirmedByPlayerProfileId,
        Instant confirmedAt,
        Long rejectedByPlayerProfileId,
        Instant rejectedAt,
        String rejectionReason
) {
}
