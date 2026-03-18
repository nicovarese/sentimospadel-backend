package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;

public record PlayerRatingUpdate(
        Long playerProfileId,
        BigDecimal oldRating,
        BigDecimal delta,
        BigDecimal newRating,
        UruguayCategory newCategory
) {
}
