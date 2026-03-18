package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import java.math.BigDecimal;

public record RatingParticipantSnapshot(
        Long playerProfileId,
        MatchParticipantTeam team,
        BigDecimal currentRating,
        int ratedMatchesCount
) {
}
