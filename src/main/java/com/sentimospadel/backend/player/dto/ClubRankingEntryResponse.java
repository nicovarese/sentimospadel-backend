package com.sentimospadel.backend.player.dto;

import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;

public record ClubRankingEntryResponse(
        Long playerProfileId,
        String fullName,
        String photoUrl,
        BigDecimal currentRating,
        UruguayCategory currentCategory,
        Integer matchesPlayedAtClub
) {
}
