package com.sentimospadel.backend.rating.service;

import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.rating.dto.RatingHistoryEntryResponse;
import com.sentimospadel.backend.rating.dto.RatingHistoryMatchSummaryResponse;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
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
        Map<Long, MatchResult> resultByMatchId = matchResultRepository.findAllByMatchIdIn(
                        historyEntries.stream().map(entry -> entry.getMatch().getId()).collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), Function.identity()));

        return historyEntries.stream()
                .map(entry -> toResponse(entry, resultByMatchId.get(entry.getMatch().getId())))
                .toList();
    }

    private RatingHistoryEntryResponse toResponse(PlayerRatingHistory historyEntry, MatchResult matchResult) {
        RatingHistoryMatchSummaryResponse matchSummary = matchResult == null
                ? null
                : new RatingHistoryMatchSummaryResponse(
                        historyEntry.getMatch().getId(),
                        historyEntry.getMatch().getStatus(),
                        historyEntry.getMatch().getScheduledAt(),
                        matchResult.getWinnerTeam(),
                        new MatchScoreResponse(matchResult.getTeamOneScore(), matchResult.getTeamTwoScore())
                );

        return new RatingHistoryEntryResponse(
                historyEntry.getId(),
                historyEntry.getMatch().getId(),
                historyEntry.getOldRating(),
                historyEntry.getDelta(),
                historyEntry.getNewRating(),
                historyEntry.getCreatedAt(),
                matchSummary
        );
    }
}
