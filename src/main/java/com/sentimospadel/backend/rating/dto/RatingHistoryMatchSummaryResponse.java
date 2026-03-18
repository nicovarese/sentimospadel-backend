package com.sentimospadel.backend.rating.dto;

import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import java.time.Instant;

public record RatingHistoryMatchSummaryResponse(
        Long matchId,
        MatchStatus matchStatus,
        Instant scheduledAt,
        MatchWinnerTeam winnerTeam,
        MatchScoreResponse score
) {
}
