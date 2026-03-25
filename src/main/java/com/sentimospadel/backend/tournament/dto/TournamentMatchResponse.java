package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import java.time.Instant;

public record TournamentMatchResponse(
        Long id,
        Long tournamentId,
        TournamentMatchPhase phase,
        TournamentMatchStatus status,
        Integer roundNumber,
        Integer legNumber,
        String roundLabel,
        Instant scheduledAt,
        String courtName,
        TournamentMatchTeamResponse teamOne,
        TournamentMatchTeamResponse teamTwo,
        boolean resultExists,
        TournamentMatchResultResponse result,
        Instant createdAt,
        Instant updatedAt
) {
}
