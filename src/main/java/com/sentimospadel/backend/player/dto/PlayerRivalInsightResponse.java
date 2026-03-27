package com.sentimospadel.backend.player.dto;

import java.math.BigDecimal;

public record PlayerRivalInsightResponse(
        Long playerProfileId,
        String fullName,
        String photoUrl,
        Integer matchesLostAgainst,
        BigDecimal ratingLostAgainst
) {
}
