package com.sentimospadel.backend.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sentimospadel.backend.onboarding.dto.InitialSurveyRequest;
import com.sentimospadel.backend.onboarding.enums.AnswerOption;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InitialSurveyCalculationServiceTest {

    private InitialSurveyCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new InitialSurveyCalculationService();
    }

    @Test
    void calculateMapsAnswersAndBuildsWeightedScore() {
        InitialSurveyRequest request = new InitialSurveyRequest(
                AnswerOption.B,
                AnswerOption.C,
                AnswerOption.D,
                AnswerOption.E,
                AnswerOption.A,
                AnswerOption.C,
                AnswerOption.B,
                AnswerOption.D,
                AnswerOption.E,
                AnswerOption.B
        );

        InitialSurveyCalculationResult result = calculationService.calculate(request);

        assertEquals(78, result.weightedScore());
        assertEquals(new BigDecimal("19.50"), result.normalizedScore());
        assertEquals(new BigDecimal("3.96"), result.initialRating());
        assertEquals(UruguayCategory.QUINTA, result.estimatedCategory());
        assertFalse(result.requiresClubVerification());
        assertEquals(ClubVerificationStatus.NOT_REQUIRED, result.clubVerificationStatus());
    }

    @Test
    void calculateAppliesQ9AntiInflationRuleWhenQ6IsBelowC() {
        InitialSurveyRequest request = new InitialSurveyRequest(
                AnswerOption.A,
                AnswerOption.A,
                AnswerOption.A,
                AnswerOption.A,
                AnswerOption.A,
                AnswerOption.B,
                AnswerOption.A,
                AnswerOption.A,
                AnswerOption.E,
                AnswerOption.A
        );

        InitialSurveyCalculationResult result = calculationService.calculate(request);

        assertEquals(8, result.weightedScore());
        assertEquals(new BigDecimal("2.00"), result.normalizedScore());
        assertEquals(new BigDecimal("1.28"), result.initialRating());
    }

    @Test
    void calculateAppliesPrimeraGate() {
        InitialSurveyRequest request = new InitialSurveyRequest(
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.C
        );

        InitialSurveyCalculationResult result = calculationService.calculate(request);

        assertEquals(new BigDecimal("6.39"), result.initialRating());
        assertEquals(UruguayCategory.SEGUNDA, result.estimatedCategory());
        assertTrue(result.requiresClubVerification());
        assertEquals(ClubVerificationStatus.PENDING, result.clubVerificationStatus());
    }

    @Test
    void calculateAppliesSegundaGate() {
        InitialSurveyRequest request = new InitialSurveyRequest(
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.B,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E,
                AnswerOption.E
        );

        InitialSurveyCalculationResult result = calculationService.calculate(request);

        assertEquals(new BigDecimal("5.49"), result.initialRating());
        assertEquals(UruguayCategory.TERCERA, result.estimatedCategory());
        assertFalse(result.requiresClubVerification());
    }

    @Test
    void calculateMapsPiecewiseRatingWithoutGates() {
        InitialSurveyRequest request = new InitialSurveyRequest(
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.C
        );

        InitialSurveyCalculationResult result = calculationService.calculate(request);

        assertEquals(80, result.weightedScore());
        assertEquals(new BigDecimal("20.00"), result.normalizedScore());
        assertEquals(new BigDecimal("4.04"), result.initialRating());
        assertEquals(UruguayCategory.QUINTA, result.estimatedCategory());
    }

    @Test
    void mapCategoryUsesConfiguredUruguayThresholds() {
        assertEquals(UruguayCategory.PRIMERA, calculationService.mapCategory(new BigDecimal("6.40")));
        assertEquals(UruguayCategory.SEGUNDA, calculationService.mapCategory(new BigDecimal("5.50")));
        assertEquals(UruguayCategory.TERCERA, calculationService.mapCategory(new BigDecimal("4.80")));
        assertEquals(UruguayCategory.CUARTA, calculationService.mapCategory(new BigDecimal("4.10")));
        assertEquals(UruguayCategory.QUINTA, calculationService.mapCategory(new BigDecimal("3.40")));
        assertEquals(UruguayCategory.SEXTA, calculationService.mapCategory(new BigDecimal("2.60")));
        assertEquals(UruguayCategory.SEPTIMA, calculationService.mapCategory(new BigDecimal("2.59")));
    }
}
