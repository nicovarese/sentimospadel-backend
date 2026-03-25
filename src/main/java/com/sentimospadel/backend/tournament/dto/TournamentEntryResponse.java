package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import java.time.Instant;
import java.util.List;

public record TournamentEntryResponse(
        Long id,
        String teamName,
        TournamentEntryStatus status,
        List<String> timePreferences,
        List<TournamentEntryMemberResponse> members,
        Instant createdAt
) {
}
