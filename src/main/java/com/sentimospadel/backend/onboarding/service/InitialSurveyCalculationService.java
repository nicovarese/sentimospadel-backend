package com.sentimospadel.backend.onboarding.service;

import com.sentimospadel.backend.onboarding.dto.InitialSurveyRequest;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class InitialSurveyCalculationService {

    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal FOURTEEN = BigDecimal.valueOf(14);
    private static final BigDecimal ELEVEN = BigDecimal.valueOf(11);
    private static final BigDecimal FIVE = BigDecimal.valueOf(5);

    public InitialSurveyCalculationResult calculate(InitialSurveyRequest request) {
        // Extract answer values once so the calculation stays explicit and matches the agreed product formula.
        int q1Value = request.q1().value();
        int q2Value = request.q2().value();
        int q3Value = request.q3().value();
        int q4Value = request.q4().value();
        int q5Value = request.q5().value();
        int q6Value = request.q6().value();
        int q7Value = request.q7().value();
        int q8Value = request.q8().value();
        int q9Value = applyQ9Rule(request.q9().value(), q6Value);
        int q10Value = request.q10().value();

        int weightedScore = (5 * q1Value)
                + (4 * q2Value)
                + (4 * q3Value)
                + (3 * q4Value)
                + (3 * q5Value)
                + (6 * q6Value)
                + (4 * q7Value)
                + (4 * q8Value)
                + (2 * q9Value)
                + (5 * q10Value);

        BigDecimal normalizedScore = BigDecimal.valueOf(weightedScore)
                .divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);

        BigDecimal rating = calculateRating(normalizedScore);
        rating = applyGates(rating, q6Value, q10Value);

        UruguayCategory estimatedCategory = mapCategory(rating);
        boolean requiresClubVerification = estimatedCategory == UruguayCategory.PRIMERA
                || estimatedCategory == UruguayCategory.SEGUNDA;

        return new InitialSurveyCalculationResult(
                weightedScore,
                normalizedScore,
                rating,
                estimatedCategory,
                requiresClubVerification,
                requiresClubVerification ? ClubVerificationStatus.PENDING : ClubVerificationStatus.NOT_REQUIRED
        );
    }

    UruguayCategory mapCategory(BigDecimal rating) {
        return UruguayCategoryMapper.fromRating(rating);
    }

    private int applyQ9Rule(int q9Value, int q6Value) {
        // Q9 self-assessment is capped when the player does not meet the minimum Q6 experience threshold.
        if (q6Value < 2) {
            return Math.min(q9Value, 1);
        }
        return q9Value;
    }

    private BigDecimal calculateRating(BigDecimal normalizedScore) {
        BigDecimal rating;

        // The onboarding score uses a piecewise curve so low, mid, and top ranges spread differently.
        if (normalizedScore.compareTo(TEN) <= 0) {
            rating = BigDecimal.valueOf(1.00)
                    .add(normalizedScore.divide(TEN, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(1.40)));
        } else if (normalizedScore.compareTo(BigDecimal.valueOf(24)) <= 0) {
            rating = BigDecimal.valueOf(2.40)
                    .add(normalizedScore.subtract(TEN)
                            .divide(FOURTEEN, 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(2.30)));
        } else if (normalizedScore.compareTo(BigDecimal.valueOf(35)) <= 0) {
            rating = BigDecimal.valueOf(4.70)
                    .add(normalizedScore.subtract(BigDecimal.valueOf(24))
                            .divide(ELEVEN, 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(1.20)));
        } else {
            rating = BigDecimal.valueOf(5.90)
                    .add(normalizedScore.subtract(BigDecimal.valueOf(35))
                            .divide(FIVE, 8, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(1.10)));
        }

        return rating.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyGates(BigDecimal rating, int q6Value, int q10Value) {
        BigDecimal gatedRating = rating;

        // Primera requires both recent competitive context (Q10) and solid level signal (Q6).
        if (gatedRating.compareTo(BigDecimal.valueOf(6.40)) >= 0 && !(q10Value >= 3 && q6Value >= 3)) {
            gatedRating = gatedRating.min(BigDecimal.valueOf(6.39));
        }

        // Segunda is never allowed when Q6 stays below the minimum threshold.
        if (gatedRating.compareTo(BigDecimal.valueOf(5.50)) >= 0 && q6Value < 2) {
            gatedRating = gatedRating.min(BigDecimal.valueOf(5.49));
        }

        return gatedRating.setScale(2, RoundingMode.HALF_UP);
    }
}
