package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;
import java.time.Instant;

public record MatchParticipantResponse(
        Long playerProfileId,
        Long userId,
        String fullName,
        BigDecimal currentRating,
        UruguayCategory currentCategory,
        Integer matchesPlayed,
        boolean requiresClubVerification,
        ClubVerificationStatus clubVerificationStatus,
        MatchParticipantTeam team,
        Instant joinedAt
) {
    public MatchParticipantResponse(
            Long playerProfileId,
            Long userId,
            String fullName,
            MatchParticipantTeam team,
            Instant joinedAt
    ) {
        this(
                playerProfileId,
                userId,
                fullName,
                null,
                null,
                null,
                false,
                ClubVerificationStatus.NOT_REQUIRED,
                team,
                joinedAt
        );
    }
}
