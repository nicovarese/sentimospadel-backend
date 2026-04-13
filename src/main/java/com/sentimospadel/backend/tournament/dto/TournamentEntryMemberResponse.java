package com.sentimospadel.backend.tournament.dto;

import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;

public record TournamentEntryMemberResponse(
        Long playerProfileId,
        Long userId,
        String fullName,
        BigDecimal currentRating,
        UruguayCategory currentCategory,
        Integer matchesPlayed,
        boolean requiresClubVerification,
        ClubVerificationStatus clubVerificationStatus
) {
    public TournamentEntryMemberResponse(
            Long playerProfileId,
            Long userId,
            String fullName
    ) {
        this(
                playerProfileId,
                userId,
                fullName,
                null,
                null,
                null,
                false,
                ClubVerificationStatus.NOT_REQUIRED
        );
    }
}
