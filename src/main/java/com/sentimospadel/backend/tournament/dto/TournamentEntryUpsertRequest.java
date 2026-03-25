package com.sentimospadel.backend.tournament.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TournamentEntryUpsertRequest(
        String teamName,
        @Size(max = 12) List<String> timePreferences,
        @NotEmpty @Size(max = 2) List<@Valid TournamentEntryMemberRequest> members
) {
}
