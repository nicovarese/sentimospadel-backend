package com.sentimospadel.backend.rating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RatingCalculationServiceTest {

    private RatingCalculationService ratingCalculationService;

    @BeforeEach
    void setUp() {
        ratingCalculationService = new RatingCalculationService();
    }

    @Test
    void expectedProbabilityGrowsWhenTeamOneHasHigherRating() {
        double strongerProbability = ratingCalculationService.expectedProbability(0.60d);
        double weakerProbability = ratingCalculationService.expectedProbability(-0.60d);

        assertTrue(strongerProbability > 0.5d);
        assertTrue(weakerProbability < 0.5d);
        assertEquals(1.0d, strongerProbability + weakerProbability, 0.000001d);
    }

    @Test
    void dynamicKUsesConfiguredExperienceBands() {
        assertEquals(0.28d, ratingCalculationService.resolveTeamK(10), 0.000001d);
        assertEquals(0.22d, ratingCalculationService.resolveTeamK(11), 0.000001d);
        assertEquals(0.12d, ratingCalculationService.resolveTeamK(26), 0.000001d);
        assertEquals(0.10d, ratingCalculationService.resolveTeamK(61), 0.000001d);
    }

    @Test
    void setBonusUsesCappedSetDifference() {
        assertEquals(0.02d, ratingCalculationService.setBonus(2, 0), 0.000001d);
        assertEquals(-0.02d, ratingCalculationService.setBonus(0, 2), 0.000001d);
        assertEquals(0.00d, ratingCalculationService.setBonus(1, 1), 0.000001d);
        assertEquals(0.02d, ratingCalculationService.setBonus(4, 0), 0.000001d);
    }

    @Test
    void comfortZoneCapShrinksTeamDeltaForExperiencedTeamOne() {
        assertEquals(0.06d, ratingCalculationService.applyTeamComfortCap(0.14d, 26), 0.000001d);
        assertEquals(0.14d, ratingCalculationService.applyTeamComfortCap(0.14d, 12), 0.000001d);
    }

    @Test
    void antiFrustrationCapLimitsNewPlayersMoreAggressively() {
        assertEquals(0.18d, ratingCalculationService.playerDeltaCap(0), 0.000001d);
        assertEquals(0.18d, ratingCalculationService.playerDeltaCap(10), 0.000001d);
        assertEquals(0.30d, ratingCalculationService.playerDeltaCap(11), 0.000001d);
    }

    @Test
    void finalizeRatingClampsToOfficialRange() {
        assertEquals(new BigDecimal("7.00"), ratingCalculationService.finalizeRating(new BigDecimal("6.95"), 0.20d));
        assertEquals(new BigDecimal("1.00"), ratingCalculationService.finalizeRating(new BigDecimal("1.03"), -0.10d));
    }

    @Test
    void categoryMappingUsesOfficialThresholds() {
        assertEquals(UruguayCategory.PRIMERA, UruguayCategoryMapper.fromRating(new BigDecimal("6.40")));
        assertEquals(UruguayCategory.SEGUNDA, UruguayCategoryMapper.fromRating(new BigDecimal("5.50")));
        assertEquals(UruguayCategory.TERCERA, UruguayCategoryMapper.fromRating(new BigDecimal("4.80")));
        assertEquals(UruguayCategory.CUARTA, UruguayCategoryMapper.fromRating(new BigDecimal("4.10")));
        assertEquals(UruguayCategory.QUINTA, UruguayCategoryMapper.fromRating(new BigDecimal("3.40")));
        assertEquals(UruguayCategory.SEXTA, UruguayCategoryMapper.fromRating(new BigDecimal("2.60")));
        assertEquals(UruguayCategory.SEPTIMA, UruguayCategoryMapper.fromRating(new BigDecimal("2.59")));
    }

    @Test
    void calculateProducesBoundedPlayerUpdatesOnOfficialScale() {
        MatchRatingCalculationResult result = ratingCalculationService.calculate(List.of(
                new RatingParticipantSnapshot(1L, MatchParticipantTeam.TEAM_ONE, new BigDecimal("6.90"), 8),
                new RatingParticipantSnapshot(2L, MatchParticipantTeam.TEAM_ONE, new BigDecimal("6.70"), 8),
                new RatingParticipantSnapshot(3L, MatchParticipantTeam.TEAM_TWO, new BigDecimal("1.20"), 40),
                new RatingParticipantSnapshot(4L, MatchParticipantTeam.TEAM_TWO, new BigDecimal("1.10"), 42)
        ), 2, 0);

        assertEquals(4, result.updates().size());
        result.updates().forEach(update -> {
            assertTrue(update.newRating().compareTo(new BigDecimal("1.00")) >= 0);
            assertTrue(update.newRating().compareTo(new BigDecimal("7.00")) <= 0);
        });
    }
}
