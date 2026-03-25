package com.sentimospadel.backend.tournament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TournamentMatchScoreSetRequest(
        @NotNull @Min(0) Integer teamOneGames,
        @NotNull @Min(0) Integer teamTwoGames
) {
}
