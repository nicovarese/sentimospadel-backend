package com.sentimospadel.backend.player.dto;

public record PlayerClubRankingSummaryResponse(
        Long clubId,
        String clubName,
        Integer matchesPlayedByUser,
        ClubRankingBucketResponse competitive,
        ClubRankingBucketResponse social
) {
}
