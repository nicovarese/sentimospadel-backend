package com.sentimospadel.backend.tournament.dto;

import java.util.List;

public record TournamentStandingsGroupResponse(
        String groupName,
        List<TournamentStandingsEntryResponse> standings
) {
}
