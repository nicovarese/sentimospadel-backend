package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
import java.util.List;

public record TournamentStandingsResponse(
        Long tournamentId,
        TournamentStandingsTiebreak tiebreak,
        List<TournamentStandingsEntryResponse> standings
) {
}
