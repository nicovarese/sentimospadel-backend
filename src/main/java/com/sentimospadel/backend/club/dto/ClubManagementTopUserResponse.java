package com.sentimospadel.backend.club.dto;

public record ClubManagementTopUserResponse(
        Integer position,
        Long playerProfileId,
        String fullName,
        String photoUrl,
        Integer matchesThisMonth
) {
}
