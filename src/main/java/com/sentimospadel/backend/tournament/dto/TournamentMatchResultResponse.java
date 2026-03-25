package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import java.time.Instant;
import java.util.List;

public record TournamentMatchResultResponse(
        Long tournamentMatchId,
        Long submittedByPlayerProfileId,
        TournamentMatchResultStatus status,
        MatchWinnerTeam winnerTeam,
        List<TournamentMatchScoreSetResponse> sets,
        Instant submittedAt,
        Long confirmedByPlayerProfileId,
        Instant confirmedAt,
        Long rejectedByPlayerProfileId,
        Instant rejectedAt,
        String rejectionReason
) {
}
