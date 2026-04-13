package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TournamentInvitePreviewResponse(
        Long tournamentId,
        String name,
        TournamentStatus status,
        TournamentFormat format,
        boolean openEnrollment,
        boolean competitive,
        String creatorName,
        Long clubId,
        String clubName,
        String city,
        List<String> categoryLabels,
        LocalDate startDate,
        LocalDate endDate,
        Integer currentEntriesCount,
        Integer currentPlayersCount,
        Integer maxEntries,
        Instant expiresAt
) {
}
