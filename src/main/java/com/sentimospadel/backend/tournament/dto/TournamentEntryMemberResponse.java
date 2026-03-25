package com.sentimospadel.backend.tournament.dto;

public record TournamentEntryMemberResponse(
        Long playerProfileId,
        Long userId,
        String fullName
) {
}
