package com.sentimospadel.backend.player.dto;

import java.util.List;

public record ClubRankingBucketResponse(
        Integer userRank,
        ClubRankingEntryResponse userEntry,
        List<ClubRankingEntryResponse> topEntries
) {
}
