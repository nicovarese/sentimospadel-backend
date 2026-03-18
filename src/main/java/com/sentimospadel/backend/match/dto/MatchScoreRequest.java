package com.sentimospadel.backend.match.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatchScoreRequest(
        @NotNull @Min(0) Integer teamOneScore,
        @NotNull @Min(0) Integer teamTwoScore
) {
}
