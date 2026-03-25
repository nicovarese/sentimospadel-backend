package com.sentimospadel.backend.tournament.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SyncTournamentEntriesRequest(
        @NotNull List<@Valid TournamentEntryUpsertRequest> entries
) {
}
