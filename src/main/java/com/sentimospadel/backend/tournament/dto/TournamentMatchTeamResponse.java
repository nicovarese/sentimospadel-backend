package com.sentimospadel.backend.tournament.dto;

import java.util.List;

public record TournamentMatchTeamResponse(
        Long tournamentEntryId,
        String teamName,
        List<TournamentEntryMemberResponse> members
) {
}
