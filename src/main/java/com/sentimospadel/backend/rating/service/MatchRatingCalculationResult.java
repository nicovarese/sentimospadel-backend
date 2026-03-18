package com.sentimospadel.backend.rating.service;

import java.util.List;

public record MatchRatingCalculationResult(
        double expectedTeamOneProbability,
        double kFactor,
        double teamOneDelta,
        List<PlayerRatingUpdate> updates
) {
}
