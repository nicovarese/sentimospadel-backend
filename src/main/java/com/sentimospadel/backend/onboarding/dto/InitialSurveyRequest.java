package com.sentimospadel.backend.onboarding.dto;

import com.sentimospadel.backend.onboarding.enums.AnswerOption;
import jakarta.validation.constraints.NotNull;

public record InitialSurveyRequest(
        @NotNull AnswerOption q1,
        @NotNull AnswerOption q2,
        @NotNull AnswerOption q3,
        @NotNull AnswerOption q4,
        @NotNull AnswerOption q5,
        @NotNull AnswerOption q6,
        @NotNull AnswerOption q7,
        @NotNull AnswerOption q8,
        @NotNull AnswerOption q9,
        @NotNull AnswerOption q10
) {
}
