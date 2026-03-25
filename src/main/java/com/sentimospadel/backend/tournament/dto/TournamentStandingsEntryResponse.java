package com.sentimospadel.backend.tournament.dto;

import java.util.List;

public record TournamentStandingsEntryResponse(
        int position,
        Long tournamentEntryId,
        String teamName,
        List<TournamentEntryMemberResponse> members,
        int points,
        int played,
        int wins,
        int losses,
        int setsWon,
        int setsLost,
        int setDifference,
        int gamesWon,
        int gamesLost,
        int gamesDifference
) {
}
