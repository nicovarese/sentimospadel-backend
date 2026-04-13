package com.sentimospadel.backend.tournament.dto;

import java.util.List;

public record TournamentLaunchPreviewResponse(
        int availableCourts,
        int numberOfGroups,
        int leagueRounds,
        List<String> courtNames,
        List<TournamentLaunchPreviewGroupResponse> groups,
        List<TournamentLaunchPreviewMatchResponse> stageMatches,
        List<TournamentLaunchPreviewMatchResponse> playoffMatches
) {
}
