package com.sentimospadel.backend.rating.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RatingHistoryEntryResponse(
        Long id,
        Long matchId,
        BigDecimal oldRating,
        BigDecimal delta,
        BigDecimal newRating,
        Instant createdAt,
        RatingHistoryMatchSummaryResponse match
) {
}
