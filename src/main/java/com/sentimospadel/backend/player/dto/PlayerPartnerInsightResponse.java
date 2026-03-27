package com.sentimospadel.backend.player.dto;

import java.math.BigDecimal;

public record PlayerPartnerInsightResponse(
        Long playerProfileId,
        String fullName,
        String photoUrl,
        Integer matchesWonTogether,
        BigDecimal ratingGainedTogether
) {
}
