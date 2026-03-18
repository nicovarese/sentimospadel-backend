package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingApplicationService {

    private final MatchResultRepository matchResultRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerRatingHistoryRepository playerRatingHistoryRepository;
    private final RatingCalculationService ratingCalculationService;

    @Transactional
    public boolean applyConfirmedResultIfNeeded(Long matchId) {
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Result for match with id " + matchId + " was not found"));

        if (result.getStatus() != MatchResultStatus.CONFIRMED) {
            return false;
        }

        if (result.isRatingApplied()) {
            return false;
        }

        long historyCount = playerRatingHistoryRepository.countByMatchId(matchId);
        if (historyCount == 4) {
            result.setRatingApplied(true);
            result.setRatingAppliedAt(result.getConfirmedAt() == null ? Instant.now() : result.getConfirmedAt());
            matchResultRepository.save(result);
            return false;
        }
        if (historyCount > 0) {
            throw new ConflictException("Rating history for this match is inconsistent");
        }

        List<MatchParticipant> participants = matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(matchId);
        validateRatedParticipants(participants);

        MatchRatingCalculationResult calculationResult = ratingCalculationService.calculate(
                participants.stream()
                        .map(this::toSnapshot)
                        .toList(),
                result.getTeamOneScore(),
                result.getTeamTwoScore()
        );

        Map<Long, PlayerRatingUpdate> updatesByPlayerId = calculationResult.updates().stream()
                .collect(Collectors.toMap(PlayerRatingUpdate::playerProfileId, update -> update));
        Instant appliedAt = Instant.now();

        participants.forEach(participant -> applyUpdate(participant.getPlayerProfile(), updatesByPlayerId.get(participant.getPlayerProfile().getId())));
        playerProfileRepository.saveAll(participants.stream().map(MatchParticipant::getPlayerProfile).toList());

        List<PlayerRatingHistory> historyRows = participants.stream()
                .map(participant -> {
                    PlayerRatingUpdate update = updatesByPlayerId.get(participant.getPlayerProfile().getId());
                    return PlayerRatingHistory.builder()
                            .playerProfile(participant.getPlayerProfile())
                            .match(result.getMatch())
                            .oldRating(update.oldRating())
                            .delta(update.delta())
                            .newRating(update.newRating())
                            .createdAt(appliedAt)
                            .build();
                })
                .toList();
        playerRatingHistoryRepository.saveAll(historyRows);

        result.setRatingApplied(true);
        result.setRatingAppliedAt(appliedAt);
        matchResultRepository.save(result);
        return true;
    }

    private RatingParticipantSnapshot toSnapshot(MatchParticipant participant) {
        PlayerProfile playerProfile = participant.getPlayerProfile();
        return new RatingParticipantSnapshot(
                playerProfile.getId(),
                participant.getTeam(),
                playerProfile.getCurrentRating(),
                playerProfile.getRatedMatchesCount()
        );
    }

    private void applyUpdate(PlayerProfile playerProfile, PlayerRatingUpdate update) {
        playerProfile.setCurrentRating(update.newRating());
        playerProfile.setMatchesPlayed(playerProfile.getMatchesPlayed() + 1);
        playerProfile.setRatedMatchesCount(playerProfile.getRatedMatchesCount() + 1);
    }

    private void validateRatedParticipants(List<MatchParticipant> participants) {
        if (participants.size() != 4) {
            throw new ConflictException("Only completed 2 versus 2 matches can update ratings");
        }

        long teamOneCount = participants.stream().filter(participant -> participant.getTeam() == MatchParticipantTeam.TEAM_ONE).count();
        long teamTwoCount = participants.stream().filter(participant -> participant.getTeam() == MatchParticipantTeam.TEAM_TWO).count();
        if (teamOneCount != 2 || teamTwoCount != 2) {
            throw new ConflictException("Ratings require explicit 2 versus 2 team assignments");
        }
    }
}
