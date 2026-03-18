package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RatingCalculationService {

    private static final double SCALE_S = 0.55d;

    public MatchRatingCalculationResult calculate(List<RatingParticipantSnapshot> participants, int teamOneSets, int teamTwoSets) {
        List<RatingParticipantSnapshot> teamOne = participants.stream()
                .filter(participant -> participant.team() == MatchParticipantTeam.TEAM_ONE)
                .sorted(Comparator.comparing(RatingParticipantSnapshot::playerProfileId))
                .toList();
        List<RatingParticipantSnapshot> teamTwo = participants.stream()
                .filter(participant -> participant.team() == MatchParticipantTeam.TEAM_TWO)
                .sorted(Comparator.comparing(RatingParticipantSnapshot::playerProfileId))
                .toList();

        if (teamOne.size() != 2 || teamTwo.size() != 2) {
            throw new BadRequestException("Confirmed rating updates require exactly 2 players per team");
        }

        if (teamOneSets == teamTwoSets) {
            throw new BadRequestException("Rated matches require a non-tied confirmed result");
        }

        double rA1 = teamOne.get(0).currentRating().doubleValue();
        double rA2 = teamOne.get(1).currentRating().doubleValue();
        double rB1 = teamTwo.get(0).currentRating().doubleValue();
        double rB2 = teamTwo.get(1).currentRating().doubleValue();

        double teamOneRating = (rA1 + rA2) / 2.0d;
        double teamTwoRating = (rB1 + rB2) / 2.0d;
        double probabilityTeamOne = expectedProbability(teamOneRating - teamTwoRating);
        double scoreTeamOne = teamOneSets > teamTwoSets ? 1.0d : 0.0d;

        int teamOneExperience = averageTeamExperience(teamOne.get(0).ratedMatchesCount(), teamOne.get(1).ratedMatchesCount());
        int teamTwoExperience = averageTeamExperience(teamTwo.get(0).ratedMatchesCount(), teamTwo.get(1).ratedMatchesCount());
        double kFactor = (resolveTeamK(teamOneExperience) + resolveTeamK(teamTwoExperience)) / 2.0d;
        double baseDeltaTeamOne = kFactor * (scoreTeamOne - probabilityTeamOne);
        double rawTeamOneDelta = baseDeltaTeamOne + setBonus(teamOneSets, teamTwoSets);
        double teamOneDelta = applyTeamComfortCap(rawTeamOneDelta, teamOneExperience);
        double teamTwoDelta = -teamOneDelta;

        PlayerRatingUpdate teamOnePlayerOne = buildPlayerUpdate(teamOne.get(0), teamOneDelta, rA1 / (rA1 + rA2));
        PlayerRatingUpdate teamOnePlayerTwo = buildPlayerUpdate(teamOne.get(1), teamOneDelta, rA2 / (rA1 + rA2));
        PlayerRatingUpdate teamTwoPlayerOne = buildPlayerUpdate(teamTwo.get(0), teamTwoDelta, rB1 / (rB1 + rB2));
        PlayerRatingUpdate teamTwoPlayerTwo = buildPlayerUpdate(teamTwo.get(1), teamTwoDelta, rB2 / (rB1 + rB2));

        return new MatchRatingCalculationResult(
                probabilityTeamOne,
                kFactor,
                teamOneDelta,
                List.of(teamOnePlayerOne, teamOnePlayerTwo, teamTwoPlayerOne, teamTwoPlayerTwo)
        );
    }

    double expectedProbability(double ratingDifference) {
        return 1.0d / (1.0d + Math.exp((-ratingDifference) / SCALE_S));
    }

    double resolveTeamK(int ratedMatchesCount) {
        if (ratedMatchesCount <= 10) {
            return 0.28d;
        }
        if (ratedMatchesCount <= 25) {
            return 0.22d;
        }
        if (ratedMatchesCount <= 60) {
            return 0.12d;
        }
        return 0.10d;
    }

    double setBonus(int teamOneSets, int teamTwoSets) {
        int cappedTeamOneSets = Math.min(teamOneSets, 2);
        int cappedTeamTwoSets = Math.min(teamTwoSets, 2);
        int difference = clampInt(cappedTeamOneSets - cappedTeamTwoSets, -2, 2);
        return 0.01d * difference;
    }

    double applyTeamComfortCap(double rawTeamDelta, int teamOneExperience) {
        double cap = teamOneExperience >= 26 && teamOneExperience <= 60 ? 0.06d : 0.30d;
        return clamp(rawTeamDelta, -cap, cap);
    }

    double playerDeltaCap(int ratedMatchesCount) {
        return ratedMatchesCount <= 10 ? 0.18d : 0.30d;
    }

    BigDecimal finalizeRating(BigDecimal oldRating, double clampedDelta) {
        double rawNewRating = oldRating.doubleValue() + clampedDelta;
        double bounded = clamp(rawNewRating, 1.0d, 7.0d);
        return BigDecimal.valueOf(bounded).setScale(2, RoundingMode.HALF_UP);
    }

    private PlayerRatingUpdate buildPlayerUpdate(RatingParticipantSnapshot participant, double teamDelta, double share) {
        double rawDelta = teamDelta * share;
        double cappedDelta = clamp(rawDelta, -playerDeltaCap(participant.ratedMatchesCount()), playerDeltaCap(participant.ratedMatchesCount()));
        BigDecimal newRating = finalizeRating(participant.currentRating(), cappedDelta);
        BigDecimal appliedDelta = newRating.subtract(participant.currentRating()).setScale(2, RoundingMode.HALF_UP);

        return new PlayerRatingUpdate(
                participant.playerProfileId(),
                participant.currentRating(),
                appliedDelta,
                newRating,
                UruguayCategoryMapper.fromRating(newRating)
        );
    }

    private int averageTeamExperience(int firstPlayerMatches, int secondPlayerMatches) {
        return (int) Math.round((firstPlayerMatches + secondPlayerMatches) / 2.0d);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
