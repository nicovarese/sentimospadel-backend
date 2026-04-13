package com.sentimospadel.backend.tournament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTournamentEntryTeamNameRequest(
        @NotBlank @Size(max = 160) String teamName
) {
}
