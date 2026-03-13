package com.sentimospadel.backend.onboarding.dto;

import com.sentimospadel.backend.onboarding.enums.AnswerOption;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;
import java.time.Instant;

public record InitialSurveyResponse(
        Long id,
        Integer surveyVersion,
        AnswerOption q1,
        AnswerOption q2,
        AnswerOption q3,
        AnswerOption q4,
        AnswerOption q5,
        AnswerOption q6,
        AnswerOption q7,
        AnswerOption q8,
        AnswerOption q9,
        AnswerOption q10,
        Integer weightedScore,
        BigDecimal normalizedScore,
        BigDecimal initialRating,
        UruguayCategory estimatedCategory,
        boolean requiresClubVerification,
        ClubVerificationStatus clubVerificationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
