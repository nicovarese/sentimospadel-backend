package com.sentimospadel.backend.tournament.dto;

import jakarta.validation.constraints.Positive;
import java.util.List;

public record LaunchTournamentRequest(
        @Positive Integer availableCourts,
        @Positive Integer numberOfGroups,
        Integer leagueRounds,
        List<String> courtNames
) {
}
