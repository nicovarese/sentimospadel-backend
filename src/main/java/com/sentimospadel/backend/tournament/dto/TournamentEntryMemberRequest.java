package com.sentimospadel.backend.tournament.dto;

import jakarta.validation.constraints.NotNull;

public record TournamentEntryMemberRequest(
        @NotNull Long playerProfileId
) {
}
