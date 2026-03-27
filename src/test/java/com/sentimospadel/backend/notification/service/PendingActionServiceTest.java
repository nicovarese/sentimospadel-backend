package com.sentimospadel.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingActionServiceTest {

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private TournamentMatchRepository tournamentMatchRepository;

    @Mock
    private TournamentMatchResultRepository tournamentMatchResultRepository;

    private PendingActionService pendingActionService;

    @BeforeEach
    void setUp() {
        pendingActionService = new PendingActionService(
                matchParticipantRepository,
                matchResultRepository,
                tournamentMatchRepository,
                tournamentMatchResultRepository
        );
    }

    @Test
    void socialMatchDoesNotCreateSubmitTaskBeforeMatchEnds() {
        PlayerProfile player = playerProfile(10L, "player@example.com");
        Match match = socialMatch(100L, Instant.now().minus(Duration.ofMinutes(20)), MatchStatus.FULL);
        MatchParticipant playerParticipation = participant(match, player, MatchParticipantTeam.TEAM_ONE);
        List<MatchParticipant> participants = List.of(
                playerParticipation,
                participant(match, playerProfile(11L, "p2@example.com"), MatchParticipantTeam.TEAM_ONE),
                participant(match, playerProfile(12L, "p3@example.com"), MatchParticipantTeam.TEAM_TWO),
                participant(match, playerProfile(13L, "p4@example.com"), MatchParticipantTeam.TEAM_TWO)
        );

        when(matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(10L))
                .thenReturn(List.of(playerParticipation));
        when(matchParticipantRepository.findAllByMatchIdInOrderByJoinedAtAsc(List.of(100L)))
                .thenReturn(participants);
        when(matchResultRepository.findAllByMatchIdIn(List.of(100L))).thenReturn(List.of());
        when(tournamentMatchRepository.findAllByParticipantPlayerProfileIdOrderByScheduledAtDesc(10L)).thenReturn(List.of());

        assertThat(pendingActionService.computePendingActions(10L)).isEmpty();
    }

    @Test
    void socialMatchCreatesSubmitTaskOnceEnded() {
        PlayerProfile player = playerProfile(10L, "player@example.com");
        Match match = socialMatch(100L, Instant.now().minus(Duration.ofMinutes(100)), MatchStatus.FULL);
        MatchParticipant playerParticipation = participant(match, player, MatchParticipantTeam.TEAM_ONE);
        List<MatchParticipant> participants = List.of(
                playerParticipation,
                participant(match, playerProfile(11L, "p2@example.com"), MatchParticipantTeam.TEAM_ONE),
                participant(match, playerProfile(12L, "p3@example.com"), MatchParticipantTeam.TEAM_TWO),
                participant(match, playerProfile(13L, "p4@example.com"), MatchParticipantTeam.TEAM_TWO)
        );

        when(matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(10L))
                .thenReturn(List.of(playerParticipation));
        when(matchParticipantRepository.findAllByMatchIdInOrderByJoinedAtAsc(List.of(100L)))
                .thenReturn(participants);
        when(matchResultRepository.findAllByMatchIdIn(List.of(100L))).thenReturn(List.of());
        when(tournamentMatchRepository.findAllByParticipantPlayerProfileIdOrderByScheduledAtDesc(10L)).thenReturn(List.of());

        List<PendingActionCandidate> actions = pendingActionService.computePendingActions(10L);

        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().type()).isEqualTo(PendingActionType.SUBMIT_MATCH_RESULT);
        assertThat(actions.getFirst().matchId()).isEqualTo(100L);
    }

    @Test
    void socialMatchCreatesConfirmTaskOnlyForOppositeTeam() {
        PlayerProfile submitter = playerProfile(10L, "submitter@example.com");
        PlayerProfile confirmer = playerProfile(12L, "confirmer@example.com");
        Match match = socialMatch(100L, Instant.now().minus(Duration.ofMinutes(100)), MatchStatus.RESULT_PENDING);
        List<MatchParticipant> participants = List.of(
                participant(match, submitter, MatchParticipantTeam.TEAM_ONE),
                participant(match, playerProfile(11L, "partner@example.com"), MatchParticipantTeam.TEAM_ONE),
                participant(match, confirmer, MatchParticipantTeam.TEAM_TWO),
                participant(match, playerProfile(13L, "rival@example.com"), MatchParticipantTeam.TEAM_TWO)
        );
        MatchResult result = MatchResult.builder()
                .match(match)
                .submittedBy(submitter)
                .status(MatchResultStatus.PENDING)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .teamOneScore(2)
                .teamTwoScore(0)
                .submittedAt(Instant.now().minus(Duration.ofMinutes(5)))
                .build();

        when(matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(12L))
                .thenReturn(List.of(participants.get(2)));
        when(matchParticipantRepository.findAllByMatchIdInOrderByJoinedAtAsc(List.of(100L)))
                .thenReturn(participants);
        when(matchResultRepository.findAllByMatchIdIn(List.of(100L))).thenReturn(List.of(result));
        when(tournamentMatchRepository.findAllByParticipantPlayerProfileIdOrderByScheduledAtDesc(12L)).thenReturn(List.of());

        List<PendingActionCandidate> actions = pendingActionService.computePendingActions(12L);

        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().type()).isEqualTo(PendingActionType.CONFIRM_MATCH_RESULT);
    }

    @Test
    void tournamentMatchCreatesSubmitTaskOnlyAfterEnd() {
        PlayerProfile player = playerProfile(20L, "league@example.com");
        Tournament tournament = Tournament.builder()
                .id(200L)
                .name("Liga Otoño")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.IN_PROGRESS)
                .startDate(LocalDate.now())
                .pointsForWin(3)
                .pointsForTiebreakLoss(1)
                .pointsForLoss(0)
                .build();
        TournamentEntry teamOne = tournamentEntry(301L, tournament, player, playerProfile(21L, "pair@example.com"));
        TournamentEntry teamTwo = tournamentEntry(302L, tournament, playerProfile(22L, "r1@example.com"), playerProfile(23L, "r2@example.com"));
        TournamentMatch match = TournamentMatch.builder()
                .id(400L)
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .status(TournamentMatchStatus.SCHEDULED)
                .scheduledAt(Instant.now().minus(Duration.ofMinutes(100)))
                .roundNumber(1)
                .roundLabel("Fecha 1")
                .build();

        when(matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(20L)).thenReturn(List.of());
        when(tournamentMatchRepository.findAllByParticipantPlayerProfileIdOrderByScheduledAtDesc(20L))
                .thenReturn(List.of(match));
        when(tournamentMatchResultRepository.findAllByTournamentMatchIdIn(List.of(400L))).thenReturn(List.of());

        List<PendingActionCandidate> actions = pendingActionService.computePendingActions(20L);

        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().type()).isEqualTo(PendingActionType.SUBMIT_TOURNAMENT_RESULT);
        assertThat(actions.getFirst().tournamentMatchId()).isEqualTo(400L);
    }

    private Match socialMatch(Long id, Instant scheduledAt, MatchStatus status) {
        return Match.builder()
                .id(id)
                .scheduledAt(scheduledAt)
                .status(status)
                .locationText("Top Padel - Cancha 1")
                .maxPlayers(4)
                .build();
    }

    private MatchParticipant participant(Match match, PlayerProfile playerProfile, MatchParticipantTeam team) {
        return MatchParticipant.builder()
                .match(match)
                .playerProfile(playerProfile)
                .team(team)
                .joinedAt(Instant.now().minus(Duration.ofHours(2)))
                .build();
    }

    private TournamentEntry tournamentEntry(Long id, Tournament tournament, PlayerProfile primary, PlayerProfile secondary) {
        return TournamentEntry.builder()
                .id(id)
                .tournament(tournament)
                .primaryPlayerProfile(primary)
                .secondaryPlayerProfile(secondary)
                .status(TournamentEntryStatus.CONFIRMED)
                .teamName("Equipo " + id)
                .build();
    }

    private PlayerProfile playerProfile(Long id, String email) {
        return PlayerProfile.builder()
                .id(id)
                .user(User.builder()
                        .id(id)
                        .email(email)
                        .passwordHash("hash")
                        .role(UserRole.PLAYER)
                        .status(UserStatus.ACTIVE)
                        .build())
                .fullName(email)
                .currentRating(BigDecimal.valueOf(4.50))
                .provisional(false)
                .matchesPlayed(10)
                .ratedMatchesCount(10)
                .surveyCompleted(true)
                .requiresClubVerification(false)
                .clubVerificationStatus(com.sentimospadel.backend.player.enums.ClubVerificationStatus.NOT_REQUIRED)
                .build();
    }
}
