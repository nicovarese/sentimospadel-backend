package com.sentimospadel.backend.coach.dto;

import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;
import java.time.Instant;

public record CoachResponse(
        Long id,
        String fullName,
        String clubName,
        BigDecimal currentRating,
        UruguayCategory currentCategory,
        int reviewsCount,
        BigDecimal averageRating,
        int hourlyRateUyu,
        String phone,
        String photoUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
