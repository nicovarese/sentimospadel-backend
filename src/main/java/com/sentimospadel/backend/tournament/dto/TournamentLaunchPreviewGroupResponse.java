package com.sentimospadel.backend.tournament.dto;

import java.util.List;

public record TournamentLaunchPreviewGroupResponse(
        String name,
        List<TournamentLaunchPreviewTeamResponse> teams
) {
}
