package com.sentimospadel.backend.tournament.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpsertMyTournamentEntryRequest(
        String teamName,
        @Positive Long secondaryPlayerProfileId,
        @Size(max = 12) List<String> timePreferences
) {
}
