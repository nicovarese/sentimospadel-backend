package com.sentimospadel.backend.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.service.ClubBookingService;
import com.sentimospadel.backend.match.dto.AssignMatchTeamsRequest;
import com.sentimospadel.backend.match.dto.CreateMatchRequest;
import com.sentimospadel.backend.match.dto.MatchScoreRequest;
import com.sentimospadel.backend.match.dto.MatchResultResponse;
import com.sentimospadel.backend.match.dto.MatchResponse;
import com.sentimospadel.backend.match.dto.MatchTeamAssignmentRequest;
import com.sentimospadel.backend.match.dto.RejectMatchResultRequest;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.match.dto.SubmitMatchResultRequest;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.rating.service.RatingApplicationService;
import com.sentimospadel.backend.shared.exception.ConflictException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private ClubBookingService clubBookingService;

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    @Mock
    private RatingApplicationService ratingApplicationService;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(
                matchRepository,
                matchParticipantRepository,
                matchResultRepository,
                clubBookingService,
                playerProfileResolverService,
                ratingApplicationService
        );
    }

    @Test
    void createMatchAddsCreatorAsFirstParticipant() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        Club club = Club.builder().name("Club").city("Montevideo").build();
        ReflectionTestUtils.setField(club, "id", 7L);

        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);
        when(clubBookingService.resolveClubBooking(7L, Instant.parse("2026-03-20T20:00:00Z"), "Rambla")).thenReturn(club);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            ReflectionTestUtils.setField(match, "id", 1L);
            match.setCreatedAt(Instant.parse("2026-03-17T10:00:00Z"));
            match.setUpdatedAt(Instant.parse("2026-03-17T10:00:00Z"));
            return match;
        });
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(List.of(
                MatchParticipant.builder()
                        .match(buildMatch(1L, creator, club, MatchStatus.OPEN))
                        .playerProfile(creator)
                        .team(null)
                        .joinedAt(Instant.parse("2026-03-17T10:00:01Z"))
                        .build()
        ));

        MatchResponse response = matchService.createMatch(
                "player@example.com",
                new CreateMatchRequest(Instant.parse("2026-03-20T20:00:00Z"), 7L, "Rambla", "Partido social")
        );

        ArgumentCaptor<MatchParticipant> participantCaptor = ArgumentCaptor.forClass(MatchParticipant.class);
        verify(matchParticipantRepository).save(participantCaptor.capture());

        assertEquals(1L, response.id());
        assertEquals(MatchStatus.OPEN, response.status());
        assertEquals(4, response.maxPlayers());
        assertEquals(1, response.currentPlayerCount());
        assertEquals(10L, participantCaptor.getValue().getPlayerProfile().getId());
    }

    @Test
    void joinMatchRejectsDuplicateParticipant() {
        PlayerProfile player = buildPlayerProfile(10L, 100L, "Player One");
        Match match = buildMatch(1L, player, null, MatchStatus.OPEN);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(player);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> matchService.joinMatch("player@example.com", 1L));
    }

    @Test
    void joinMatchMarksMatchAsFullWhenFourthPlayerJoins() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile joiningPlayer = buildPlayerProfile(11L, 101L, "Player Two");
        Match match = buildMatch(1L, creator, null, MatchStatus.OPEN);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("other@example.com")).thenReturn(joiningPlayer);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 11L)).thenReturn(false);
        when(matchParticipantRepository.countByMatchId(1L)).thenReturn(3L);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(List.of(
                buildParticipant(match, creator, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, buildPlayerProfile(12L, 102L, "Player Three"), MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, buildPlayerProfile(13L, 103L, "Player Four"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:02:00Z")),
                buildParticipant(match, joiningPlayer, MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"))
        ));

        MatchResponse response = matchService.joinMatch("other@example.com", 1L);

        assertEquals(MatchStatus.FULL, response.status());
        assertEquals(4, response.currentPlayerCount());
        verify(matchRepository).save(match);
    }

    @Test
    void leaveMatchReopensFullMatchWhenPlayerCountDrops() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile leavingPlayer = buildPlayerProfile(11L, 101L, "Player Two");
        Match match = buildMatch(1L, creator, null, MatchStatus.FULL);
        MatchParticipant participant = buildParticipant(match, leavingPlayer, MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"));

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("leave@example.com")).thenReturn(leavingPlayer);
        when(matchParticipantRepository.findByMatchIdAndPlayerProfileId(1L, 11L)).thenReturn(Optional.of(participant));
        when(matchParticipantRepository.countByMatchId(1L)).thenReturn(3L);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(List.of(
                buildParticipant(match, creator, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, buildPlayerProfile(12L, 102L, "Player Three"), MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, buildPlayerProfile(13L, 103L, "Player Four"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:02:00Z"))
        ));

        MatchResponse response = matchService.leaveMatch("leave@example.com", 1L);

        assertEquals(MatchStatus.OPEN, response.status());
        assertEquals(3, response.currentPlayerCount());
        verify(matchParticipantRepository).delete(participant);
        verify(matchRepository).save(match);
    }

    @Test
    void cancelMatchAllowsCreatorToCancelOpenMatch() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        Match match = buildMatch(1L, creator, null, MatchStatus.OPEN);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);
        when(matchRepository.save(match)).thenReturn(match);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(List.of(
                buildParticipant(match, creator, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z"))
        ));

        MatchResponse response = matchService.cancelMatch("player@example.com", 1L);

        assertEquals(MatchStatus.CANCELLED, response.status());
        verify(matchRepository).save(match);
    }

    @Test
    void cancelMatchRejectsCompletedMatch() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        Match match = buildMatch(1L, creator, null, MatchStatus.COMPLETED);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);

        assertThrows(ConflictException.class, () -> matchService.cancelMatch("player@example.com", 1L));
    }

    @Test
    void cancelMatchRejectsNonCreator() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile otherPlayer = buildPlayerProfile(11L, 101L, "Player Two");
        Match match = buildMatch(1L, creator, null, MatchStatus.OPEN);

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("other@example.com")).thenReturn(otherPlayer);

        assertThrows(AccessDeniedException.class, () -> matchService.cancelMatch("other@example.com", 1L));
    }

    @Test
    void assignTeamsRequiresCompleteValidTwoVsTwoDistribution() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile playerTwo = buildPlayerProfile(11L, 101L, "Player Two");
        PlayerProfile playerThree = buildPlayerProfile(12L, 102L, "Player Three");
        PlayerProfile playerFour = buildPlayerProfile(13L, 103L, "Player Four");
        Match match = buildMatch(1L, creator, null, MatchStatus.FULL);

        List<MatchParticipant> participants = List.of(
                buildParticipant(match, creator, null, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, playerTwo, null, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, playerThree, null, Instant.parse("2026-03-17T10:02:00Z")),
                buildParticipant(match, playerFour, null, Instant.parse("2026-03-17T10:03:00Z"))
        );

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(participants);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.empty());

        MatchResponse response = matchService.assignTeams("player@example.com", 1L, new AssignMatchTeamsRequest(List.of(
                new MatchTeamAssignmentRequest(10L, MatchParticipantTeam.TEAM_ONE),
                new MatchTeamAssignmentRequest(11L, MatchParticipantTeam.TEAM_ONE),
                new MatchTeamAssignmentRequest(12L, MatchParticipantTeam.TEAM_TWO),
                new MatchTeamAssignmentRequest(13L, MatchParticipantTeam.TEAM_TWO)
        )));

        assertEquals(MatchParticipantTeam.TEAM_ONE, response.participants().get(0).team());
        verify(matchParticipantRepository).saveAll(any());
    }

    @Test
    void submitResultMovesMatchToResultPending() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        Match match = buildMatch(1L, creator, null, MatchStatus.FULL);
        List<MatchParticipant> participants = List.of(
                buildParticipant(match, creator, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, buildPlayerProfile(11L, 101L, "Player Two"), MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, buildPlayerProfile(12L, 102L, "Player Three"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:02:00Z")),
                buildParticipant(match, buildPlayerProfile(13L, 103L, "Player Four"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"))
        );

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 10L)).thenReturn(true);
        when(matchParticipantRepository.countByMatchId(1L)).thenReturn(4L);
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(participants);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.empty());
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResultResponse response = matchService.submitResult(
                "player@example.com",
                1L,
                new SubmitMatchResultRequest(MatchWinnerTeam.TEAM_ONE, new MatchScoreRequest(6, 3))
        );

        assertEquals(1L, response.matchId());
        assertEquals(MatchResultStatus.PENDING, response.status());
        assertEquals(MatchWinnerTeam.TEAM_ONE, response.winnerTeam());
        assertEquals(MatchStatus.RESULT_PENDING, match.getStatus());
        verify(matchRepository).save(match);
        verify(ratingApplicationService, never()).applyConfirmedResultIfNeeded(anyLong());
    }

    @Test
    void submitResultRejectsMatchWithoutPlayableTeams() {
        PlayerProfile creator = buildPlayerProfile(10L, 100L, "Player One");
        Match match = buildMatch(1L, creator, null, MatchStatus.FULL);
        List<MatchParticipant> participants = List.of(
                buildParticipant(match, creator, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, buildPlayerProfile(11L, 101L, "Player Two"), MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, buildPlayerProfile(12L, 102L, "Player Three"), null, Instant.parse("2026-03-17T10:02:00Z")),
                buildParticipant(match, buildPlayerProfile(13L, 103L, "Player Four"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"))
        );

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 10L)).thenReturn(true);
        when(matchParticipantRepository.countByMatchId(1L)).thenReturn(4L);
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(participants);

        assertThrows(ConflictException.class, () -> matchService.submitResult(
                "player@example.com",
                1L,
                new SubmitMatchResultRequest(MatchWinnerTeam.TEAM_ONE, new MatchScoreRequest(6, 3))
        ));
    }

    @Test
    void confirmResultRequiresOppositeTeamParticipant() {
        PlayerProfile submitter = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile confirmer = buildPlayerProfile(12L, 102L, "Player Three");
        Match match = buildMatch(1L, submitter, null, MatchStatus.RESULT_PENDING);
        MatchResult result = MatchResult.builder()
                .match(match)
                .submittedBy(submitter)
                .status(MatchResultStatus.PENDING)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .teamOneScore(6)
                .teamTwoScore(3)
                .submittedAt(Instant.parse("2026-03-17T10:15:00Z"))
                .build();
        List<MatchParticipant> participants = List.of(
                buildParticipant(match, submitter, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, buildPlayerProfile(11L, 101L, "Player Two"), MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, confirmer, MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:02:00Z")),
                buildParticipant(match, buildPlayerProfile(13L, 103L, "Player Four"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"))
        );

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("confirm@example.com")).thenReturn(confirmer);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(participants);
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResultResponse response = matchService.confirmResult("confirm@example.com", 1L);

        assertEquals(MatchResultStatus.CONFIRMED, response.status());
        assertEquals(12L, response.confirmedByPlayerProfileId());
        assertEquals(MatchStatus.COMPLETED, match.getStatus());
        verify(ratingApplicationService).applyConfirmedResultIfNeeded(1L);
    }

    @Test
    void rejectResultRequiresOppositeTeamParticipantAndReturnsMatchToFull() {
        PlayerProfile submitter = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile rejector = buildPlayerProfile(12L, 102L, "Player Three");
        Match match = buildMatch(1L, submitter, null, MatchStatus.RESULT_PENDING);
        MatchResult result = MatchResult.builder()
                .match(match)
                .submittedBy(submitter)
                .status(MatchResultStatus.PENDING)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .teamOneScore(6)
                .teamTwoScore(3)
                .submittedAt(Instant.parse("2026-03-17T10:15:00Z"))
                .build();
        List<MatchParticipant> participants = List.of(
                buildParticipant(match, submitter, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, buildPlayerProfile(11L, 101L, "Player Two"), MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, rejector, MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:02:00Z")),
                buildParticipant(match, buildPlayerProfile(13L, 103L, "Player Four"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"))
        );

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("reject@example.com")).thenReturn(rejector);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(participants);
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResultResponse response = matchService.rejectResult(
                "reject@example.com",
                1L,
                new RejectMatchResultRequest("Score mal cargado")
        );

        assertEquals(MatchResultStatus.REJECTED, response.status());
        assertEquals(12L, response.rejectedByPlayerProfileId());
        assertEquals("Score mal cargado", response.rejectionReason());
        assertEquals(MatchStatus.FULL, match.getStatus());
        verifyNoInteractions(ratingApplicationService);
    }

    @Test
    void rejectResultDoesNotAllowConfirmedResults() {
        PlayerProfile submitter = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile rejector = buildPlayerProfile(12L, 102L, "Player Three");
        Match match = buildMatch(1L, submitter, null, MatchStatus.COMPLETED);
        MatchResult result = MatchResult.builder()
                .match(match)
                .submittedBy(submitter)
                .status(MatchResultStatus.CONFIRMED)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .teamOneScore(6)
                .teamTwoScore(3)
                .submittedAt(Instant.parse("2026-03-17T10:15:00Z"))
                .build();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("reject@example.com")).thenReturn(rejector);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(result));

        assertThrows(ConflictException.class, () -> matchService.rejectResult(
                "reject@example.com",
                1L,
                new RejectMatchResultRequest("No corresponde")
        ));
    }

    @Test
    void submitResultAllowsResubmissionAfterRejection() {
        PlayerProfile originalSubmitter = buildPlayerProfile(10L, 100L, "Player One");
        PlayerProfile resubmitter = buildPlayerProfile(12L, 102L, "Player Three");
        Match match = buildMatch(1L, originalSubmitter, null, MatchStatus.FULL);
        MatchResult rejectedResult = MatchResult.builder()
                .match(match)
                .submittedBy(originalSubmitter)
                .status(MatchResultStatus.REJECTED)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .teamOneScore(6)
                .teamTwoScore(3)
                .submittedAt(Instant.parse("2026-03-17T10:15:00Z"))
                .rejectedBy(buildPlayerProfile(13L, 103L, "Player Four"))
                .rejectedAt(Instant.parse("2026-03-17T10:20:00Z"))
                .rejectionReason("Score mal cargado")
                .build();
        ReflectionTestUtils.setField(rejectedResult, "id", 20L);
        List<MatchParticipant> participants = List.of(
                buildParticipant(match, originalSubmitter, MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                buildParticipant(match, buildPlayerProfile(11L, 101L, "Player Two"), MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                buildParticipant(match, resubmitter, MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:02:00Z")),
                buildParticipant(match, buildPlayerProfile(13L, 103L, "Player Four"), MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"))
        );

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("resubmit@example.com")).thenReturn(resubmitter);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 12L)).thenReturn(true);
        when(matchParticipantRepository.countByMatchId(1L)).thenReturn(4L);
        when(matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(1L)).thenReturn(participants);
        when(matchResultRepository.findByMatchId(1L)).thenReturn(Optional.of(rejectedResult));
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResultResponse response = matchService.submitResult(
                "resubmit@example.com",
                1L,
                new SubmitMatchResultRequest(MatchWinnerTeam.TEAM_TWO, new MatchScoreRequest(4, 6))
        );

        assertEquals(MatchResultStatus.PENDING, response.status());
        assertEquals(12L, response.submittedByPlayerProfileId());
        assertEquals(MatchWinnerTeam.TEAM_TWO, response.winnerTeam());
        assertEquals(MatchStatus.RESULT_PENDING, match.getStatus());
        assertEquals(null, response.rejectedByPlayerProfileId());
        assertEquals(null, response.rejectionReason());
        verify(matchRepository).save(match);
        verify(matchResultRepository, never()).existsByMatchId(1L);
    }

    private PlayerProfile buildPlayerProfile(Long id, Long userId, String fullName) {
        PlayerProfile profile = PlayerProfile.builder()
                .fullName(fullName)
                .currentRating(new BigDecimal("1.00"))
                .provisional(true)
                .matchesPlayed(0)
                .ratedMatchesCount(0)
                .build();
        ReflectionTestUtils.setField(profile, "id", id);
        com.sentimospadel.backend.user.entity.User user = com.sentimospadel.backend.user.entity.User.builder()
                .email(fullName.toLowerCase().replace(' ', '.') + "@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        profile.setUser(user);
        return profile;
    }

    private Match buildMatch(Long id, PlayerProfile creator, Club club, MatchStatus status) {
        Match match = Match.builder()
                .createdBy(creator)
                .status(status)
                .scheduledAt(Instant.parse("2026-03-20T20:00:00Z"))
                .club(club)
                .locationText("Rambla")
                .notes("Partido social")
                .maxPlayers(4)
                .build();
        ReflectionTestUtils.setField(match, "id", id);
        match.setCreatedAt(Instant.parse("2026-03-17T10:00:00Z"));
        match.setUpdatedAt(Instant.parse("2026-03-17T10:00:00Z"));
        return match;
    }

    private MatchParticipant buildParticipant(Match match, PlayerProfile playerProfile, MatchParticipantTeam team, Instant joinedAt) {
        return MatchParticipant.builder()
                .match(match)
                .playerProfile(playerProfile)
                .team(team)
                .joinedAt(joinedAt)
                .build();
    }
}
