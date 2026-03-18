package com.sentimospadel.backend.match.service;

import com.sentimospadel.backend.match.dto.MatchParticipantResponse;
import com.sentimospadel.backend.match.dto.MatchResultSummaryResponse;
import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.dto.PlayerMatchHistoryEntryResponse;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerMatchHistoryService {

    private final PlayerProfileResolverService playerProfileResolverService;
    private final PlayerProfileRepository playerProfileRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchResultRepository matchResultRepository;

    @Transactional(readOnly = true)
    public List<PlayerMatchHistoryEntryResponse> getMyMatches(String email) {
        Long userId = playerProfileResolverService.getUserByEmail(email).getId();

        return playerProfileRepository.findByUserId(userId)
                .map(playerProfile -> getMatchesForPlayerProfile(playerProfile.getId()))
                .orElseGet(Collections::emptyList);
    }

    @Transactional(readOnly = true)
    public List<PlayerMatchHistoryEntryResponse> getMatchesForPlayerProfile(Long playerProfileId) {
        List<MatchParticipant> playerParticipations =
                matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(playerProfileId);

        if (playerParticipations.isEmpty()) {
            return List.of();
        }

        List<Match> matches = playerParticipations.stream()
                .map(MatchParticipant::getMatch)
                .toList();
        List<Long> matchIds = matches.stream().map(Match::getId).toList();

        Map<Long, List<MatchParticipant>> participantsByMatchId = matchParticipantRepository
                .findAllByMatchIdInOrderByJoinedAtAsc(matchIds)
                .stream()
                .collect(Collectors.groupingBy(participant -> participant.getMatch().getId()));

        Map<Long, MatchResult> resultByMatchId = matchResultRepository.findAllByMatchIdIn(matchIds)
                .stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), Function.identity()));

        return playerParticipations.stream()
                .map(playerParticipation -> toResponse(
                        playerParticipation,
                        participantsByMatchId.getOrDefault(playerParticipation.getMatch().getId(), List.of()),
                        resultByMatchId.get(playerParticipation.getMatch().getId())
                ))
                .toList();
    }

    private PlayerMatchHistoryEntryResponse toResponse(
            MatchParticipant playerParticipation,
            List<MatchParticipant> participants,
            MatchResult result
    ) {
        Match match = playerParticipation.getMatch();
        List<MatchParticipantResponse> participantResponses = participants.stream()
                .map(participant -> new MatchParticipantResponse(
                        participant.getPlayerProfile().getId(),
                        participant.getPlayerProfile().getUser().getId(),
                        participant.getPlayerProfile().getFullName(),
                        participant.getTeam(),
                        participant.getJoinedAt()
                ))
                .toList();

        MatchResultSummaryResponse resultSummary = result == null
                ? null
                : new MatchResultSummaryResponse(
                        result.getStatus(),
                        result.getWinnerTeam(),
                        new MatchScoreResponse(result.getTeamOneScore(), result.getTeamTwoScore()),
                        result.getSubmittedAt(),
                        result.getSubmittedBy().getId(),
                        result.getConfirmedBy() == null ? null : result.getConfirmedBy().getId(),
                        result.getConfirmedAt(),
                        result.getRejectedBy() == null ? null : result.getRejectedBy().getId(),
                        result.getRejectedAt(),
                        result.getRejectionReason()
                );

        MatchParticipantTeam authenticatedPlayerTeam = playerParticipation.getTeam();

        return new PlayerMatchHistoryEntryResponse(
                match.getId(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getClub() == null ? null : match.getClub().getId(),
                match.getLocationText(),
                match.getNotes(),
                participantResponses.size(),
                participantResponses,
                resultSummary != null,
                resultSummary,
                true,
                authenticatedPlayerTeam,
                derivePlayerWin(result, authenticatedPlayerTeam),
                match.getCreatedAt(),
                match.getUpdatedAt()
        );
    }

    private Boolean derivePlayerWin(MatchResult result, MatchParticipantTeam playerTeam) {
        if (result == null || result.getStatus() != MatchResultStatus.CONFIRMED || playerTeam == null) {
            return null;
        }

        MatchWinnerTeam winnerTeam = result.getWinnerTeam();
        if (winnerTeam == null) {
            return null;
        }

        return (winnerTeam == MatchWinnerTeam.TEAM_ONE && playerTeam == MatchParticipantTeam.TEAM_ONE)
                || (winnerTeam == MatchWinnerTeam.TEAM_TWO && playerTeam == MatchParticipantTeam.TEAM_TWO);
    }
}
