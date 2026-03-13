package com.sentimospadel.backend.player.dto;

import com.sentimospadel.backend.player.enums.PreferredSide;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;
import java.time.Instant;

public record PlayerProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String photoUrl,
        PreferredSide preferredSide,
        String declaredLevel,
        String city,
        String bio,
        Integer currentElo,
        boolean provisional,
        Integer matchesPlayed,
        boolean surveyCompleted,
        Instant surveyCompletedAt,
        BigDecimal initialRating,
        UruguayCategory estimatedCategory,
        boolean requiresClubVerification,
        ClubVerificationStatus clubVerificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
