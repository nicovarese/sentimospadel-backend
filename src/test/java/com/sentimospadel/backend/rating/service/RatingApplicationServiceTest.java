package com.sentimospadel.backend.rating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RatingApplicationServiceTest {

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private PlayerRatingHistoryRepository playerRatingHistoryRepository;

    private RatingCalculationService ratingCalculationService;
    private RatingApplicationService ratingApplicationService;

    @BeforeEach
    void setUp() {
        ratingCalculationService = new RatingCalculationService();
        ratingApplicationService = new RatingApplicationService(
                matchResultRepository,
                matchParticipantRepository,
                playerProfileRepository,
                playerRatingHistoryRepository,
                ratingCalculationService
        );
    }

    @Test
    void confirmedResultAppliesRatingOnceForAllPlayers() {
        Match match = buildMatch(1L);
        MatchResult result = buildResult(match, MatchResultStatus.CONFIRMED, false);
        List<MatchParticipant> participants = buildParticipants(match);

        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));
        when(playerRatingHistoryRepository.countByMatchId(1L)).thenReturn(0L);
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(participants);
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean applied = ratingApplicationService.applyConfirmedResultIfNeeded(1L);

        assertTrue(applied);
        assertTrue(result.isRatingApplied());
        assertTrue(participants.get(0).getPlayerProfile().getCurrentRating().compareTo(new BigDecimal("4.80")) > 0);
        assertEquals(1, participants.get(0).getPlayerProfile().getRatedMatchesCount());
        assertEquals(1, participants.get(0).getPlayerProfile().getMatchesPlayed());

        ArgumentCaptor<List<PlayerRatingHistory>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingHistoryRepository).saveAll(historyCaptor.capture());
        assertEquals(4, historyCaptor.getValue().size());
    }

    @Test
    void pendingResultDoesNotApplyRating() {
        Match match = buildMatch(1L);
        MatchResult result = buildResult(match, MatchResultStatus.PENDING, false);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));

        boolean applied = ratingApplicationService.applyConfirmedResultIfNeeded(1L);

        assertFalse(applied);
        verify(playerProfileRepository, never()).saveAll(any());
        verify(playerRatingHistoryRepository, never()).saveAll(any());
    }

    @Test
    void rejectedResultDoesNotApplyRating() {
        Match match = buildMatch(1L);
        MatchResult result = buildResult(match, MatchResultStatus.REJECTED, false);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));

        boolean applied = ratingApplicationService.applyConfirmedResultIfNeeded(1L);

        assertFalse(applied);
        verify(playerProfileRepository, never()).saveAll(any());
        verify(playerRatingHistoryRepository, never()).saveAll(any());
    }

    @Test
    void alreadyAppliedConfirmedResultIsIgnored() {
        Match match = buildMatch(1L);
        MatchResult result = buildResult(match, MatchResultStatus.CONFIRMED, true);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));

        boolean applied = ratingApplicationService.applyConfirmedResultIfNeeded(1L);

        assertFalse(applied);
        verify(playerProfileRepository, never()).saveAll(any());
        verify(playerRatingHistoryRepository, never()).saveAll(any());
    }

    private Match buildMatch(Long id) {
        Match match = Match.builder()
                .status(MatchStatus.COMPLETED)
                .scheduledAt(Instant.parse("2026-03-20T20:00:00Z"))
                .maxPlayers(4)
                .build();
        ReflectionTestUtils.setField(match, "id", id);
        return match;
    }

    private MatchResult buildResult(Match match, MatchResultStatus status, boolean ratingApplied) {
        MatchResult result = MatchResult.builder()
                .match(match)
                .submittedBy(buildProfile(10L, "Player One", "4.80", 0))
                .status(status)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .teamOneScore(2)
                .teamTwoScore(1)
                .submittedAt(Instant.parse("2026-03-17T10:00:00Z"))
                .confirmedAt(Instant.parse("2026-03-17T10:10:00Z"))
                .ratingApplied(ratingApplied)
                .build();
        ReflectionTestUtils.setField(result, "id", 20L);
        return result;
    }

    private List<MatchParticipant> buildParticipants(Match match) {
        return List.of(
                buildParticipant(match, buildProfile(10L, "Player One", "4.80", 0), MatchParticipantTeam.TEAM_ONE),
                buildParticipant(match, buildProfile(11L, "Player Two", "4.60", 2), MatchParticipantTeam.TEAM_ONE),
                buildParticipant(match, buildProfile(12L, "Player Three", "4.90", 4), MatchParticipantTeam.TEAM_TWO),
                buildParticipant(match, buildProfile(13L, "Player Four", "4.70", 6), MatchParticipantTeam.TEAM_TWO)
        );
    }

    private MatchParticipant buildParticipant(Match match, PlayerProfile playerProfile, MatchParticipantTeam team) {
        return MatchParticipant.builder()
                .match(match)
                .playerProfile(playerProfile)
                .team(team)
                .joinedAt(Instant.parse("2026-03-17T09:00:00Z"))
                .build();
    }

    private PlayerProfile buildProfile(Long id, String fullName, String rating, int ratedMatchesCount) {
        PlayerProfile profile = PlayerProfile.builder()
                .fullName(fullName)
                .currentRating(new BigDecimal(rating))
                .provisional(true)
                .matchesPlayed(ratedMatchesCount)
                .ratedMatchesCount(ratedMatchesCount)
                .build();
        ReflectionTestUtils.setField(profile, "id", id);
        return profile;
    }
}
