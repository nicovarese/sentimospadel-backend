package com.sentimospadel.backend.rating.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RatingHistoryEntryResponse(
        Long id,
        Long matchId,
        Long tournamentMatchId,
        RatingHistorySourceType sourceType,
        BigDecimal oldRating,
        BigDecimal delta,
        BigDecimal newRating,
        Instant createdAt,
        RatingHistoryMatchSummaryResponse match,
        RatingHistoryTournamentMatchSummaryResponse tournamentMatch
) {
}
