package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.tournament.enums.TournamentAmericanoType;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TournamentResponse(
        Long id,
        Long createdByPlayerProfileId,
        String name,
        String description,
        Long clubId,
        String city,
        LocalDate startDate,
        LocalDate endDate,
        TournamentStatus status,
        TournamentFormat format,
        TournamentAmericanoType americanoType,
        boolean openEnrollment,
        boolean competitive,
        boolean archived,
        Integer maxEntries,
        int currentEntriesCount,
        int currentPlayersCount,
        Integer availableCourts,
        Integer numberOfGroups,
        Integer leagueRounds,
        Integer matchesPerParticipant,
        TournamentStandingsTiebreak standingsTiebreak,
        List<String> courtNames,
        Instant launchedAt,
        Instant archivedAt,
        boolean affectsPlayerRating,
        int generatedMatchesCount,
        List<TournamentEntryResponse> entries,
        Instant createdAt,
        Instant updatedAt
) {
}
