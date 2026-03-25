package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmitTournamentMatchResultRequest(
        @NotNull MatchWinnerTeam winnerTeam,
        @NotEmpty @Size(max = 3) List<@Valid TournamentMatchScoreSetRequest> sets
) {
}
