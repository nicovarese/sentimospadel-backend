package com.sentimospadel.backend.player.service;

import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.dto.ClubRankingBucketResponse;
import com.sentimospadel.backend.player.dto.ClubRankingEntryResponse;
import com.sentimospadel.backend.player.dto.PlayerClubRankingSummaryResponse;
import com.sentimospadel.backend.player.dto.PlayerPartnerInsightResponse;
import com.sentimospadel.backend.player.dto.PlayerRivalInsightResponse;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerInsightService {

    private static final int CLUB_RANKING_LIMIT = 7;
    private static final int PARTNER_RIVAL_LIMIT = 10;
    private static final Duration PLAYED_MATCH_DURATION = Duration.ofMinutes(90);

    private final PlayerProfileResolverService playerProfileResolverService;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchResultRepository matchResultRepository;
    private final PlayerRatingHistoryRepository playerRatingHistoryRepository;

    @Transactional(readOnly = true)
    public List<PlayerPartnerInsightResponse> getMyTopPartners(String email) {
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);
        MatchInsightContext context = loadSocialMatchInsightContext(playerProfile.getId());
        return buildTopPartners(playerProfile.getId(), context);
    }

    @Transactional(readOnly = true)
    public List<PlayerRivalInsightResponse> getMyTopRivals(String email) {
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);
        MatchInsightContext context = loadSocialMatchInsightContext(playerProfile.getId());
        return buildTopRivals(playerProfile.getId(), context);
    }

    @Transactional(readOnly = true)
    public List<PlayerClubRankingSummaryResponse> getMyClubRankings(String email) {
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        List<MatchParticipant> playerParticipations = matchParticipantRepository
                .findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(playerProfile.getId());

        Set<Long> clubIds = playerParticipations.stream()
                .map(participation -> participation.getMatch().getClub())
                .filter(Objects::nonNull)
                .map(club -> club.getId())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        if (clubIds.isEmpty()) {
            return List.of();
        }

        List<MatchParticipant> clubParticipants = matchParticipantRepository
                .findAllByMatchClubIdInOrderByMatchScheduledAtDesc(clubIds);

        Map<Long, Map<Long, ClubPlayerAccumulator>> clubPlayerStats = new LinkedHashMap<>();
        Instant now = Instant.now();

        for (MatchParticipant participation : clubParticipants) {
            if (participation.getMatch().getClub() == null) {
                continue;
            }

            if (!countAsPlayedClubMatch(participation, now)) {
                continue;
            }

            Long clubId = participation.getMatch().getClub().getId();
            clubPlayerStats.computeIfAbsent(clubId, ignored -> new LinkedHashMap<>());

            Map<Long, ClubPlayerAccumulator> playersAtClub = clubPlayerStats.get(clubId);
            Long profileId = participation.getPlayerProfile().getId();
            playersAtClub.computeIfAbsent(profileId, ignored -> ClubPlayerAccumulator.from(participation.getPlayerProfile()))
                    .incrementMatches();
        }

        return clubIds.stream()
                .map(clubId -> toClubRankingSummary(clubId, playerProfile.getId(), clubPlayerStats.getOrDefault(clubId, Map.of()), playerParticipations))
                .filter(Objects::nonNull)
                .toList();
    }

    private MatchInsightContext loadSocialMatchInsightContext(Long playerProfileId) {
        List<MatchParticipant> playerParticipations = matchParticipantRepository
                .findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(playerProfileId);

        if (playerParticipations.isEmpty()) {
            return MatchInsightContext.empty();
        }

        List<Long> matchIds = playerParticipations.stream()
                .map(participation -> participation.getMatch().getId())
                .distinct()
                .toList();

        Map<Long, MatchParticipant> myParticipationByMatchId = playerParticipations.stream()
                .collect(Collectors.toMap(participation -> participation.getMatch().getId(), Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<Long, List<MatchParticipant>> participantsByMatchId = matchParticipantRepository
                .findAllByMatchIdInOrderByJoinedAtAsc(matchIds)
                .stream()
                .collect(Collectors.groupingBy(
                        participation -> participation.getMatch().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        Map<Long, MatchResult> confirmedResultsByMatchId = matchResultRepository.findAllByMatchIdIn(matchIds)
                .stream()
                .filter(result -> result.getStatus() == MatchResultStatus.CONFIRMED)
                .collect(Collectors.toMap(result -> result.getMatch().getId(), Function.identity()));

        Map<Long, PlayerRatingHistory> historyByMatchId = playerRatingHistoryRepository
                .findAllByPlayerProfileIdOrderByCreatedAtDesc(playerProfileId)
                .stream()
                .collect(Collectors.toMap(history -> history.getMatch().getId(), Function.identity(), (left, right) -> left));

        return new MatchInsightContext(myParticipationByMatchId, participantsByMatchId, confirmedResultsByMatchId, historyByMatchId);
    }

    private List<PlayerPartnerInsightResponse> buildTopPartners(Long playerProfileId, MatchInsightContext context) {
        Map<Long, PartnerAccumulator> partners = new HashMap<>();

        for (Map.Entry<Long, MatchParticipant> entry : context.myParticipationByMatchId.entrySet()) {
            MatchResult result = context.confirmedResultsByMatchId.get(entry.getKey());
            if (result == null) {
                continue;
            }

            MatchParticipant myParticipation = entry.getValue();
            if (myParticipation.getTeam() == null || !didTeamWin(myParticipation.getTeam(), result.getWinnerTeam())) {
                continue;
            }

            BigDecimal gained = positiveDelta(context.historyByMatchId.get(entry.getKey()));
            List<MatchParticipant> teammates = context.participantsByMatchId.getOrDefault(entry.getKey(), List.of())
                    .stream()
                    .filter(participant -> participant.getTeam() == myParticipation.getTeam())
                    .filter(participant -> !participant.getPlayerProfile().getId().equals(playerProfileId))
                    .toList();

            for (MatchParticipant teammate : teammates) {
                partners.computeIfAbsent(teammate.getPlayerProfile().getId(), ignored -> PartnerAccumulator.from(teammate.getPlayerProfile()))
                        .registerWin(gained);
            }
        }

        return partners.values().stream()
                .sorted(Comparator
                        .comparingInt(PartnerAccumulator::matchesWonTogether).reversed()
                        .thenComparing(PartnerAccumulator::ratingGainedTogether, Comparator.reverseOrder())
                        .thenComparing(PartnerAccumulator::fullName))
                .limit(PARTNER_RIVAL_LIMIT)
                .map(PartnerAccumulator::toResponse)
                .toList();
    }

    private List<PlayerRivalInsightResponse> buildTopRivals(Long playerProfileId, MatchInsightContext context) {
        Map<Long, RivalAccumulator> rivals = new HashMap<>();

        for (Map.Entry<Long, MatchParticipant> entry : context.myParticipationByMatchId.entrySet()) {
            MatchResult result = context.confirmedResultsByMatchId.get(entry.getKey());
            if (result == null) {
                continue;
            }

            MatchParticipant myParticipation = entry.getValue();
            if (myParticipation.getTeam() == null || didTeamWin(myParticipation.getTeam(), result.getWinnerTeam())) {
                continue;
            }

            BigDecimal lost = negativeDeltaMagnitude(context.historyByMatchId.get(entry.getKey()));
            List<MatchParticipant> opponents = context.participantsByMatchId.getOrDefault(entry.getKey(), List.of())
                    .stream()
                    .filter(participant -> participant.getTeam() != null)
                    .filter(participant -> participant.getTeam() != myParticipation.getTeam())
                    .toList();

            for (MatchParticipant opponent : opponents) {
                rivals.computeIfAbsent(opponent.getPlayerProfile().getId(), ignored -> RivalAccumulator.from(opponent.getPlayerProfile()))
                        .registerLoss(lost);
            }
        }

        return rivals.values().stream()
                .sorted(Comparator
                        .comparingInt(RivalAccumulator::matchesLostAgainst).reversed()
                        .thenComparing(RivalAccumulator::ratingLostAgainst, Comparator.reverseOrder())
                        .thenComparing(RivalAccumulator::fullName))
                .limit(PARTNER_RIVAL_LIMIT)
                .map(RivalAccumulator::toResponse)
                .toList();
    }

    private PlayerClubRankingSummaryResponse toClubRankingSummary(
            Long clubId,
            Long currentPlayerProfileId,
            Map<Long, ClubPlayerAccumulator> playersAtClub,
            List<MatchParticipant> playerParticipations
    ) {
        if (playersAtClub.isEmpty()) {
            return null;
        }

        String clubName = playerParticipations.stream()
                .map(MatchParticipant::getMatch)
                .filter(match -> match.getClub() != null && clubId.equals(match.getClub().getId()))
                .map(match -> match.getClub().getName())
                .findFirst()
                .orElse("Club");

        List<ClubRankingEntryResponse> competitiveEntries = playersAtClub.values().stream()
                .map(ClubPlayerAccumulator::toResponse)
                .sorted(Comparator
                        .comparing(ClubRankingEntryResponse::currentRating, Comparator.reverseOrder())
                        .thenComparing(ClubRankingEntryResponse::fullName))
                .toList();

        List<ClubRankingEntryResponse> socialEntries = playersAtClub.values().stream()
                .map(ClubPlayerAccumulator::toResponse)
                .sorted(Comparator
                        .comparing(ClubRankingEntryResponse::matchesPlayedAtClub, Comparator.reverseOrder())
                        .thenComparing(ClubRankingEntryResponse::currentRating, Comparator.reverseOrder())
                        .thenComparing(ClubRankingEntryResponse::fullName))
                .toList();

        ClubRankingBucketResponse competitive = toBucket(currentPlayerProfileId, competitiveEntries);
        ClubRankingBucketResponse social = toBucket(currentPlayerProfileId, socialEntries);

        if (competitive.userEntry() == null || social.userEntry() == null) {
            return null;
        }

        return new PlayerClubRankingSummaryResponse(
                clubId,
                clubName,
                social.userEntry().matchesPlayedAtClub(),
                competitive,
                social
        );
    }

    private ClubRankingBucketResponse toBucket(Long currentPlayerProfileId, List<ClubRankingEntryResponse> entries) {
        ClubRankingEntryResponse userEntry = null;
        Integer userRank = null;

        for (int index = 0; index < entries.size(); index++) {
            ClubRankingEntryResponse entry = entries.get(index);
            if (entry.playerProfileId().equals(currentPlayerProfileId)) {
                userEntry = entry;
                userRank = index + 1;
                break;
            }
        }

        return new ClubRankingBucketResponse(
                userRank,
                userEntry,
                entries.stream().limit(CLUB_RANKING_LIMIT).toList()
        );
    }

    private boolean countAsPlayedClubMatch(MatchParticipant participation, Instant now) {
        if (participation.getMatch().getStatus() == MatchStatus.CANCELLED) {
            return false;
        }

        return participation.getMatch().getScheduledAt().plus(PLAYED_MATCH_DURATION).isBefore(now)
                || participation.getMatch().getStatus() == MatchStatus.COMPLETED
                || participation.getMatch().getStatus() == MatchStatus.RESULT_PENDING;
    }

    private boolean didTeamWin(MatchParticipantTeam team, MatchWinnerTeam winnerTeam) {
        return (team == MatchParticipantTeam.TEAM_ONE && winnerTeam == MatchWinnerTeam.TEAM_ONE)
                || (team == MatchParticipantTeam.TEAM_TWO && winnerTeam == MatchWinnerTeam.TEAM_TWO);
    }

    private BigDecimal positiveDelta(PlayerRatingHistory history) {
        if (history == null || history.getDelta().signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return history.getDelta().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal negativeDeltaMagnitude(PlayerRatingHistory history) {
        if (history == null || history.getDelta().signum() >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return history.getDelta().abs().setScale(2, RoundingMode.HALF_UP);
    }

    private record MatchInsightContext(
            Map<Long, MatchParticipant> myParticipationByMatchId,
            Map<Long, List<MatchParticipant>> participantsByMatchId,
            Map<Long, MatchResult> confirmedResultsByMatchId,
            Map<Long, PlayerRatingHistory> historyByMatchId
    ) {
        private static MatchInsightContext empty() {
            return new MatchInsightContext(Map.of(), Map.of(), Map.of(), Map.of());
        }
    }

    private static final class PartnerAccumulator {

        private final Long playerProfileId;
        private final String fullName;
        private final String photoUrl;
        private int matchesWonTogether;
        private BigDecimal ratingGainedTogether;

        private PartnerAccumulator(Long playerProfileId, String fullName, String photoUrl) {
            this.playerProfileId = playerProfileId;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
            this.matchesWonTogether = 0;
            this.ratingGainedTogether = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        private static PartnerAccumulator from(PlayerProfile profile) {
            return new PartnerAccumulator(profile.getId(), profile.getFullName(), profile.getPhotoUrl());
        }

        private void registerWin(BigDecimal gained) {
            matchesWonTogether++;
            ratingGainedTogether = ratingGainedTogether.add(gained).setScale(2, RoundingMode.HALF_UP);
        }

        private int matchesWonTogether() {
            return matchesWonTogether;
        }

        private BigDecimal ratingGainedTogether() {
            return ratingGainedTogether;
        }

        private String fullName() {
            return fullName;
        }

        private PlayerPartnerInsightResponse toResponse() {
            return new PlayerPartnerInsightResponse(
                    playerProfileId,
                    fullName,
                    photoUrl,
                    matchesWonTogether,
                    ratingGainedTogether
            );
        }
    }

    private static final class RivalAccumulator {

        private final Long playerProfileId;
        private final String fullName;
        private final String photoUrl;
        private int matchesLostAgainst;
        private BigDecimal ratingLostAgainst;

        private RivalAccumulator(Long playerProfileId, String fullName, String photoUrl) {
            this.playerProfileId = playerProfileId;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
            this.matchesLostAgainst = 0;
            this.ratingLostAgainst = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        private static RivalAccumulator from(PlayerProfile profile) {
            return new RivalAccumulator(profile.getId(), profile.getFullName(), profile.getPhotoUrl());
        }

        private void registerLoss(BigDecimal lost) {
            matchesLostAgainst++;
            ratingLostAgainst = ratingLostAgainst.add(lost).setScale(2, RoundingMode.HALF_UP);
        }

        private int matchesLostAgainst() {
            return matchesLostAgainst;
        }

        private BigDecimal ratingLostAgainst() {
            return ratingLostAgainst;
        }

        private String fullName() {
            return fullName;
        }

        private PlayerRivalInsightResponse toResponse() {
            return new PlayerRivalInsightResponse(
                    playerProfileId,
                    fullName,
                    photoUrl,
                    matchesLostAgainst,
                    ratingLostAgainst
            );
        }
    }

    private static final class ClubPlayerAccumulator {

        private final Long playerProfileId;
        private final String fullName;
        private final String photoUrl;
        private final BigDecimal currentRating;
        private final com.sentimospadel.backend.player.enums.UruguayCategory currentCategory;
        private int matchesPlayedAtClub;

        private ClubPlayerAccumulator(
                Long playerProfileId,
                String fullName,
                String photoUrl,
                BigDecimal currentRating,
                com.sentimospadel.backend.player.enums.UruguayCategory currentCategory
        ) {
            this.playerProfileId = playerProfileId;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
            this.currentRating = currentRating.setScale(2, RoundingMode.HALF_UP);
            this.currentCategory = currentCategory;
            this.matchesPlayedAtClub = 0;
        }

        private static ClubPlayerAccumulator from(PlayerProfile profile) {
            return new ClubPlayerAccumulator(
                    profile.getId(),
                    profile.getFullName(),
                    profile.getPhotoUrl(),
                    profile.getCurrentRating(),
                    UruguayCategoryMapper.fromRating(profile.getCurrentRating())
            );
        }

        private void incrementMatches() {
            matchesPlayedAtClub++;
        }

        private ClubRankingEntryResponse toResponse() {
            return new ClubRankingEntryResponse(
                    playerProfileId,
                    fullName,
                    photoUrl,
                    currentRating,
                    currentCategory,
                    matchesPlayedAtClub
            );
        }
    }
}
