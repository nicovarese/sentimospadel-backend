package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import java.time.Instant;

public record TournamentLaunchPreviewMatchResponse(
        TournamentMatchPhase phase,
        String roundLabel,
        String teamOneLabel,
        String teamTwoLabel,
        Instant scheduledAt,
        String courtName,
        boolean placeholder
) {
}
