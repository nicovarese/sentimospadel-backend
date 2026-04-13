package com.sentimospadel.backend.rating.dto;

import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.tournament.dto.TournamentMatchTeamResponse;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import java.time.Instant;

public record RatingHistoryTournamentMatchSummaryResponse(
        Long tournamentMatchId,
        Long tournamentId,
        String tournamentName,
        TournamentMatchPhase phase,
        TournamentMatchStatus matchStatus,
        String roundLabel,
        Instant scheduledAt,
        MatchWinnerTeam winnerTeam,
        MatchScoreResponse score,
        TournamentMatchTeamResponse teamOne,
        TournamentMatchTeamResponse teamTwo
) {
}
