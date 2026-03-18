package com.sentimospadel.backend.rating.dto;

import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;

public record RankingEntryResponse(
        int position,
        Long playerProfileId,
        String fullName,
        String city,
        BigDecimal currentRating,
        UruguayCategory currentCategory,
        Integer ratedMatchesCount
) {
}
