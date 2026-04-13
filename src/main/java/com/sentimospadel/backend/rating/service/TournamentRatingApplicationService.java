package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentRatingApplicationService {

    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerRatingHistoryRepository playerRatingHistoryRepository;
    private final TournamentMatchResultRepository tournamentMatchResultRepository;
    private final RatingCalculationService ratingCalculationService;

    @Transactional
    public boolean applyConfirmedCompetitiveResultIfNeeded(TournamentMatchResult result) {
        TournamentMatch match = result.getTournamentMatch();
        Tournament tournament = match.getTournament();

        if (result.getStatus() != TournamentMatchResultStatus.CONFIRMED) {
            return false;
        }
        if (tournament.getFormat() != TournamentFormat.ELIMINATION || !tournament.isCompetitive()) {
            return false;
        }
        if (result.isRatingApplied()) {
            return false;
        }

        long historyCount = playerRatingHistoryRepository.countByTournamentMatchId(match.getId());
        if (historyCount == 4) {
            result.setRatingApplied(true);
            result.setRatingAppliedAt(result.getConfirmedAt() == null ? Instant.now() : result.getConfirmedAt());
            tournamentMatchResultRepository.save(result);
            return false;
        }
        if (historyCount > 0) {
            throw new ConflictException("Rating history for this tournament match is inconsistent");
        }

        List<TeamPlayer> participants = buildParticipants(match);
        MatchRatingCalculationResult calculationResult = ratingCalculationService.calculate(
                participants.stream()
                        .map(teamPlayer -> new RatingParticipantSnapshot(
                                teamPlayer.playerProfile().getId(),
                                teamPlayer.team(),
                                teamPlayer.playerProfile().getCurrentRating(),
                                teamPlayer.playerProfile().getRatedMatchesCount()
                        ))
                        .toList(),
                countSetsWon(result, true),
                countSetsWon(result, false)
        );

        Map<Long, PlayerRatingUpdate> updatesByPlayerId = calculationResult.updates().stream()
                .collect(Collectors.toMap(PlayerRatingUpdate::playerProfileId, update -> update));
        Instant appliedAt = Instant.now();

        participants.forEach(teamPlayer -> applyUpdate(teamPlayer.playerProfile(), updatesByPlayerId.get(teamPlayer.playerProfile().getId())));
        playerProfileRepository.saveAll(participants.stream().map(TeamPlayer::playerProfile).toList());

        List<PlayerRatingHistory> historyRows = participants.stream()
                .map(teamPlayer -> {
                    PlayerRatingUpdate update = updatesByPlayerId.get(teamPlayer.playerProfile().getId());
                    return PlayerRatingHistory.builder()
                            .playerProfile(teamPlayer.playerProfile())
                            .tournamentMatch(match)
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
        tournamentMatchResultRepository.save(result);
        return true;
    }

    private List<TeamPlayer> buildParticipants(TournamentMatch match) {
        List<TeamPlayer> participants = new ArrayList<>();
        addTeamParticipants(participants, match.getTeamOneEntry(), MatchParticipantTeam.TEAM_ONE);
        addTeamParticipants(participants, match.getTeamTwoEntry(), MatchParticipantTeam.TEAM_TWO);

        long teamOneCount = participants.stream().filter(participant -> participant.team() == MatchParticipantTeam.TEAM_ONE).count();
        long teamTwoCount = participants.stream().filter(participant -> participant.team() == MatchParticipantTeam.TEAM_TWO).count();
        if (teamOneCount != 2 || teamTwoCount != 2) {
            throw new ConflictException("Competitive elimination ratings require confirmed 2 versus 2 teams");
        }

        return participants;
    }

    private void addTeamParticipants(List<TeamPlayer> participants, TournamentEntry entry, MatchParticipantTeam team) {
        if (entry.getPrimaryPlayerProfile() != null) {
            participants.add(new TeamPlayer(entry.getPrimaryPlayerProfile(), team));
        }
        if (entry.getSecondaryPlayerProfile() != null) {
            participants.add(new TeamPlayer(entry.getSecondaryPlayerProfile(), team));
        }
    }

    private int countSetsWon(TournamentMatchResult result, boolean teamOne) {
        int setsWon = 0;
        Integer[][] sets = {
                {result.getSetOneTeamOneGames(), result.getSetOneTeamTwoGames()},
                {result.getSetTwoTeamOneGames(), result.getSetTwoTeamTwoGames()},
                {result.getSetThreeTeamOneGames(), result.getSetThreeTeamTwoGames()}
        };

        for (Integer[] set : sets) {
            if (set[0] == null || set[1] == null) {
                continue;
            }
            if (teamOne && set[0] > set[1]) {
                setsWon++;
            }
            if (!teamOne && set[1] > set[0]) {
                setsWon++;
            }
        }

        return setsWon;
    }

    private void applyUpdate(PlayerProfile playerProfile, PlayerRatingUpdate update) {
        playerProfile.setCurrentRating(update.newRating());
        playerProfile.setMatchesPlayed(playerProfile.getMatchesPlayed() + 1);
        playerProfile.setRatedMatchesCount(playerProfile.getRatedMatchesCount() + 1);
    }

    private record TeamPlayer(PlayerProfile playerProfile, MatchParticipantTeam team) {
    }
}
