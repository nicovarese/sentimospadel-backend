package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.tournament.enums.TournamentAmericanoType;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateTournamentRequest(
        @NotBlank String name,
        String description,
        Long clubId,
        String city,
        @Size(max = 7) List<String> categoryLabels,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull TournamentFormat format,
        TournamentAmericanoType americanoType,
        @Positive Integer maxEntries,
        Boolean openEnrollment,
        Boolean competitive,
        Integer leagueRounds,
        @Positive Integer matchesPerParticipant,
        TournamentStandingsTiebreak standingsTiebreak,
        Integer availableCourts,
        @Size(max = 20) List<String> courtNames,
        @Valid List<TournamentEntryUpsertRequest> entries
) {
}
