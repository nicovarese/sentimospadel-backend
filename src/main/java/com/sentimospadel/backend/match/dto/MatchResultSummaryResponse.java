package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import java.time.Instant;

public record MatchResultSummaryResponse(
        MatchResultStatus status,
        MatchWinnerTeam winnerTeam,
        MatchScoreResponse score,
        Instant submittedAt,
        Long submittedByPlayerProfileId,
        Long confirmedByPlayerProfileId,
        Instant confirmedAt,
        Long rejectedByPlayerProfileId,
        Instant rejectedAt,
        String rejectionReason
) {
}
