package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SubmitMatchResultRequest(
        @NotNull MatchWinnerTeam winnerTeam,
        @NotNull @Valid MatchScoreRequest score
) {
}
