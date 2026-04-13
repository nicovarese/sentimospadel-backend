package com.sentimospadel.backend.tournament.dto;

import java.util.List;

public record TournamentLaunchPreviewTeamResponse(
        String teamName,
        List<String> memberNames
) {
}
