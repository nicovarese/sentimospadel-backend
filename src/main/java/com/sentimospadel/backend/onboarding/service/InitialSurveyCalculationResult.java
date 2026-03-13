package com.sentimospadel.backend.onboarding.service;

import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;

public record InitialSurveyCalculationResult(
        int weightedScore,
        BigDecimal normalizedScore,
        BigDecimal initialRating,
        UruguayCategory estimatedCategory,
        boolean requiresClubVerification,
        ClubVerificationStatus clubVerificationStatus
) {
}
