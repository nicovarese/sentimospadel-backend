package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.rating.dto.RatingHistoryEntryResponse;
import com.sentimospadel.backend.rating.dto.RatingHistoryMatchSummaryResponse;
import com.sentimospadel.backend.rating.dto.RatingHistorySourceType;
import com.sentimospadel.backend.rating.dto.RatingHistoryTournamentMatchSummaryResponse;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import com.sentimospadel.backend.tournament.service.TournamentMapper;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerRatingHistoryService {

    private final PlayerProfileResolverService playerProfileResolverService;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerRatingHistoryRepository playerRatingHistoryRepository;
    private final MatchResultRepository matchResultRepository;
    private final TournamentMatchResultRepository tournamentMatchResultRepository;
    private final TournamentMapper tournamentMapper;

    @Transactional(readOnly = true)
    public List<RatingHistoryEntryResponse> getMyRatingHistory(String email) {
        Long userId = playerProfileResolverService.getUserByEmail(email).getId();
        PlayerProfile playerProfile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile for the authenticated user was not found"));

        return getRatingHistoryForPlayer(playerProfile.getId());
    }

    @Transactional(readOnly = true)
    public List<RatingHistoryEntryResponse> getRatingHistoryForPlayer(Long playerProfileId) {
        playerProfileRepository.findById(playerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile with id " + playerProfileId + " was not found"));

        List<PlayerRatingHistory> historyEntries = playerRatingHistoryRepository.findAllByPlayerProfileIdOrderByCreatedAtDesc(playerProfileId);
        java.util.Set<Long> socialMatchIds = historyEntries.stream()
                .map(PlayerRatingHistory::getMatch)
                .filter(match -> match != null)
                .map(match -> match.getId())
                .collect(Collectors.toSet());
        java.util.Set<Long> tournamentMatchIds = historyEntries.stream()
                .map(PlayerRatingHistory::getTournamentMatch)
                .filter(match -> match != null)
                .map(TournamentMatch::getId)
                .collect(Collectors.toSet());

        Map<Long, MatchResult> resultByMatchId = socialMatchIds.isEmpty()
                ? Map.of()
                : matchResultRepository.findAllByMatchIdIn(socialMatchIds)
                        .stream()
                        .collect(Collectors.toMap(result -> result.getMatch().getId(), Function.identity()));
        Map<Long, TournamentMatchResult> tournamentResultByMatchId = tournamentMatchIds.isEmpty()
                ? Map.of()
                : tournamentMatchResultRepository.findAllByTournamentMatchIdIn(tournamentMatchIds)
                        .stream()
                        .collect(Collectors.toMap(result -> result.getTournamentMatch().getId(), Function.identity(), (left, right) -> left));

        return historyEntries.stream()
                .map(entry -> toResponse(
                        entry,
                        entry.getMatch() == null ? null : resultByMatchId.get(entry.getMatch().getId()),
                        entry.getTournamentMatch() == null ? null : tournamentResultByMatchId.get(entry.getTournamentMatch().getId())
                ))
                .toList();
    }

    private RatingHistoryEntryResponse toResponse(
            PlayerRatingHistory historyEntry,
            MatchResult matchResult,
            TournamentMatchResult tournamentMatchResult
    ) {
        RatingHistoryMatchSummaryResponse matchSummary = matchResult == null
                ? null
                : new RatingHistoryMatchSummaryResponse(
                        historyEntry.getMatch().getId(),
                        historyEntry.getMatch().getStatus(),
                        historyEntry.getMatch().getScheduledAt(),
                        matchResult.getWinnerTeam(),
                        new MatchScoreResponse(matchResult.getTeamOneScore(), matchResult.getTeamTwoScore())
                );
        RatingHistoryTournamentMatchSummaryResponse tournamentMatchSummary = buildTournamentMatchSummary(
                historyEntry.getTournamentMatch(),
                tournamentMatchResult
        );

        return new RatingHistoryEntryResponse(
                historyEntry.getId(),
                historyEntry.getMatch() == null ? null : historyEntry.getMatch().getId(),
                historyEntry.getTournamentMatch() == null ? null : historyEntry.getTournamentMatch().getId(),
                historyEntry.getTournamentMatch() == null
                        ? RatingHistorySourceType.SOCIAL_MATCH
                        : RatingHistorySourceType.TOURNAMENT_MATCH,
                historyEntry.getOldRating(),
                historyEntry.getDelta(),
                historyEntry.getNewRating(),
                historyEntry.getCreatedAt(),
                matchSummary,
                tournamentMatchSummary
        );
    }

    private RatingHistoryTournamentMatchSummaryResponse buildTournamentMatchSummary(
            TournamentMatch tournamentMatch,
            TournamentMatchResult tournamentMatchResult
    ) {
        if (tournamentMatch == null) {
            return null;
        }

        MatchScoreResponse score = tournamentMatchResult == null
                ? null
                : new MatchScoreResponse(
                        countSetsWon(tournamentMatchResult, true),
                        countSetsWon(tournamentMatchResult, false)
                );

        return new RatingHistoryTournamentMatchSummaryResponse(
                tournamentMatch.getId(),
                tournamentMatch.getTournament().getId(),
                tournamentMatch.getTournament().getName(),
                tournamentMatch.getPhase(),
                tournamentMatch.getStatus(),
                tournamentMatch.getRoundLabel(),
                tournamentMatch.getScheduledAt(),
                tournamentMatchResult == null ? null : tournamentMatchResult.getWinnerTeam(),
                score,
                tournamentMapper.toMatchTeamResponse(tournamentMatch.getTeamOneEntry()),
                tournamentMapper.toMatchTeamResponse(tournamentMatch.getTeamTwoEntry())
        );
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
}
