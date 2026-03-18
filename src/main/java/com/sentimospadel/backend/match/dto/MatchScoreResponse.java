package com.sentimospadel.backend.match.dto;

public record MatchScoreResponse(
        Integer teamOneScore,
        Integer teamTwoScore
) {
}
