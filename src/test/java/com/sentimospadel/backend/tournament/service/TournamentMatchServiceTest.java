package com.sentimospadel.backend.tournament.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.notification.service.PlayerEventNotificationService;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.rating.service.TournamentRatingApplicationService;
import com.sentimospadel.backend.tournament.dto.TournamentEntryMemberResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResultResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchScoreSetRequest;
import com.sentimospadel.backend.tournament.dto.SubmitTournamentMatchResultRequest;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsEntryResponse;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsGroupResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentAmericanoType;
import com.sentimospadel.backend.tournament.enums.TournamentEntryKind;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentMatchServiceTest {

    @Mock
    private TournamentService tournamentService;

    @Mock
    private TournamentMatchRepository tournamentMatchRepository;

    @Mock
    private TournamentMatchResultRepository tournamentMatchResultRepository;

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    @Mock
    private TournamentMapper tournamentMapper;

    @Mock
    private TournamentStandingsService tournamentStandingsService;

    @Mock
    private PlayerEventNotificationService playerEventNotificationService;

    @Mock
    private TournamentRatingApplicationService tournamentRatingApplicationService;

    @InjectMocks
    private TournamentMatchService tournamentMatchService;

    @BeforeEach
    void setUp() {
        when(tournamentMapper.toMatchResultResponse(any(TournamentMatchResult.class)))
                .thenAnswer(invocation -> {
                    TournamentMatchResult result = invocation.getArgument(0);
                    return new TournamentMatchResultResponse(
                            result.getTournamentMatch().getId(),
                            result.getSubmittedBy().getId(),
                            result.getStatus(),
                            result.getWinnerTeam(),
                            List.of(),
                            result.getSubmittedAt(),
                            result.getConfirmedBy() == null ? null : result.getConfirmedBy().getId(),
                            result.getConfirmedAt(),
                            result.getRejectedBy() == null ? null : result.getRejectedBy().getId(),
                            result.getRejectedAt(),
                            result.getRejectionReason()
                    );
                });
    }

    @Test
    void confirmResultOnLastEliminationGroupMatchGeneratesSemifinals() {
        Tournament tournament = Tournament.builder()
                .id(300L)
                .name("Eliminatoria QA")
                .format(TournamentFormat.ELIMINATION)
                .status(TournamentStatus.IN_PROGRESS)
                .startDate(LocalDate.of(2026, 4, 10))
                .availableCourts(2)
                .courtNames(List.of("Cancha 1", "Cancha 2"))
                .build();

        PlayerProfile groupAWinner = player(11L, "A1");
        PlayerProfile groupBLoserSubmitter = player(14L, "B2");
        PlayerProfile confirmer = player(13L, "B1");

        TournamentEntry entryA1 = entry(401L, tournament, "Grupo A", "Equipo A1", groupAWinner, player(21L, "A1-2"));
        TournamentEntry entryA2 = entry(402L, tournament, "Grupo A", "Equipo A2", player(22L, "A2-1"), player(23L, "A2-2"));
        TournamentEntry entryB1 = entry(403L, tournament, "Grupo B", "Equipo B1", confirmer, player(24L, "B1-2"));
        TournamentEntry entryB2 = entry(404L, tournament, "Grupo B", "Equipo B2", player(25L, "B2-1"), groupBLoserSubmitter);

        TournamentMatch completedGroupA = TournamentMatch.builder()
                .id(501L)
                .tournament(tournament)
                .teamOneEntry(entryA1)
                .teamTwoEntry(entryA2)
                .phase(TournamentMatchPhase.GROUP_STAGE)
                .status(TournamentMatchStatus.COMPLETED)
                .roundNumber(1)
                .roundLabel("Fase de Grupos - Grupo A")
                .scheduledAt(Instant.parse("2026-04-10T18:00:00Z"))
                .courtName("Cancha 1")
                .build();

        TournamentMatch pendingGroupB = TournamentMatch.builder()
                .id(502L)
                .tournament(tournament)
                .teamOneEntry(entryB1)
                .teamTwoEntry(entryB2)
                .phase(TournamentMatchPhase.GROUP_STAGE)
                .status(TournamentMatchStatus.RESULT_PENDING)
                .roundNumber(2)
                .roundLabel("Fase de Grupos - Grupo B")
                .scheduledAt(Instant.parse("2026-04-10T20:00:00Z"))
                .courtName("Cancha 2")
                .build();

        TournamentMatchResult existingResult = TournamentMatchResult.builder()
                .id(601L)
                .tournamentMatch(pendingGroupB)
                .submittedBy(groupBLoserSubmitter)
                .status(TournamentMatchResultStatus.PENDING)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .setOneTeamOneGames(6)
                .setOneTeamTwoGames(4)
                .setTwoTeamOneGames(6)
                .setTwoTeamTwoGames(3)
                .submittedAt(Instant.parse("2026-04-10T22:00:00Z"))
                .build();

        TournamentMatch scheduledSemifinalPlaceholder = TournamentMatch.builder()
                .id(900L)
                .tournament(tournament)
                .teamOneEntry(entryA1)
                .teamTwoEntry(entryB2)
                .phase(TournamentMatchPhase.SEMIFINAL)
                .status(TournamentMatchStatus.SCHEDULED)
                .roundNumber(3)
                .roundLabel("Semifinal 1")
                .scheduledAt(Instant.parse("2026-04-11T18:00:00Z"))
                .courtName("Cancha 1")
                .build();

        when(tournamentService.getTournamentEntity(300L)).thenReturn(tournament);
        when(tournamentService.getEntries(300L)).thenReturn(List.of(entryA1, entryA2, entryB1, entryB2));
        when(playerProfileResolverService.getOrCreateByUserEmail("b1@example.com")).thenReturn(confirmer);
        when(tournamentMatchRepository.findByIdAndTournamentId(502L, 300L)).thenReturn(Optional.of(pendingGroupB));
        when(tournamentMatchResultRepository.findByTournamentMatchId(502L)).thenReturn(Optional.of(existingResult));
        when(tournamentMatchRepository.findAllByTournamentIdOrderByRoundNumberAscIdAsc(300L))
                .thenReturn(
                        List.of(completedGroupA, pendingGroupB),
                        List.of(completedGroupA, pendingGroupB, scheduledSemifinalPlaceholder)
                );
        when(tournamentStandingsService.buildEliminationGroups(tournament)).thenReturn(List.of(
                new TournamentStandingsGroupResponse(
                        "Grupo A",
                        List.of(
                                standingsEntry(1, entryA1, "Equipo A1"),
                                standingsEntry(2, entryA2, "Equipo A2")
                        )
                ),
                new TournamentStandingsGroupResponse(
                        "Grupo B",
                        List.of(
                                standingsEntry(1, entryB1, "Equipo B1"),
                                standingsEntry(2, entryB2, "Equipo B2")
                        )
                )
        ));
        when(tournamentMatchResultRepository.save(any(TournamentMatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentMatchRepository.save(any(TournamentMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentMatchRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentMatchResultResponse response = tournamentMatchService.confirmResult("b1@example.com", 300L, 502L);

        assertEquals(TournamentMatchResultStatus.CONFIRMED, response.status());
        assertEquals(TournamentMatchStatus.COMPLETED, pendingGroupB.getStatus());
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(playerEventNotificationService).notifyTournamentResultConfirmed(pendingGroupB);
        verify(tournamentRatingApplicationService).applyConfirmedCompetitiveResultIfNeeded(existingResult);

        ArgumentCaptor<List<TournamentMatch>> generatedMatchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(tournamentMatchRepository).saveAll(generatedMatchesCaptor.capture());
        List<TournamentMatch> generatedSemifinals = generatedMatchesCaptor.getValue();

        assertEquals(2, generatedSemifinals.size());
        assertTrue(generatedSemifinals.stream().allMatch(match -> match.getPhase() == TournamentMatchPhase.SEMIFINAL));
        assertTrue(generatedSemifinals.stream().allMatch(match -> match.getStatus() == TournamentMatchStatus.SCHEDULED));

        TournamentMatch semifinalOne = generatedSemifinals.get(0);
        TournamentMatch semifinalTwo = generatedSemifinals.get(1);

        assertNotNull(semifinalOne.getScheduledAt());
        assertNotNull(semifinalTwo.getScheduledAt());
        assertEquals("Equipo A1", semifinalOne.getTeamOneEntry().getTeamName());
        assertEquals("Equipo B2", semifinalOne.getTeamTwoEntry().getTeamName());
        assertEquals("Equipo B1", semifinalTwo.getTeamOneEntry().getTeamName());
        assertEquals("Equipo A2", semifinalTwo.getTeamTwoEntry().getTeamName());
    }

    @Test
    void submitResultAllowsAmericanoFijoMatches() {
        Tournament tournament = Tournament.builder()
                .id(310L)
                .name("Americano QA")
                .format(TournamentFormat.AMERICANO)
                .americanoType(TournamentAmericanoType.FIXED)
                .status(TournamentStatus.IN_PROGRESS)
                .startDate(LocalDate.of(2026, 4, 10))
                .build();

        PlayerProfile submitter = player(31L, "Americano One");
        TournamentEntry teamOne = entry(701L, tournament, null, "Equipo Uno", submitter, player(32L, "Americano Two"));
        TournamentEntry teamTwo = entry(702L, tournament, null, "Equipo Dos", player(33L, "Americano Three"), player(34L, "Americano Four"));

        TournamentMatch scheduledMatch = TournamentMatch.builder()
                .id(801L)
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .phase(TournamentMatchPhase.AMERICANO_STAGE)
                .status(TournamentMatchStatus.SCHEDULED)
                .roundNumber(1)
                .roundLabel("Ronda 1")
                .scheduledAt(Instant.parse("2026-03-01T18:00:00Z"))
                .courtName("Cancha 1")
                .build();

        SubmitTournamentMatchResultRequest request = new SubmitTournamentMatchResultRequest(
                MatchWinnerTeam.TEAM_ONE,
                List.of(
                        new TournamentMatchScoreSetRequest(6, 3),
                        new TournamentMatchScoreSetRequest(6, 4)
                )
        );

        when(tournamentService.getTournamentEntity(310L)).thenReturn(tournament);
        when(playerProfileResolverService.getOrCreateByUserEmail("americano@example.com")).thenReturn(submitter);
        when(tournamentMatchRepository.findByIdAndTournamentId(801L, 310L)).thenReturn(Optional.of(scheduledMatch));
        when(tournamentMatchResultRepository.findByTournamentMatchId(801L)).thenReturn(Optional.empty());
        when(tournamentMatchResultRepository.save(any(TournamentMatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentMatchRepository.save(any(TournamentMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentMatchResultResponse response = tournamentMatchService.submitResult("americano@example.com", 310L, 801L, request);

        assertEquals(TournamentMatchResultStatus.PENDING, response.status());
        assertEquals(TournamentMatchStatus.RESULT_PENDING, scheduledMatch.getStatus());
        verify(tournamentMatchResultRepository).save(any(TournamentMatchResult.class));
        verify(tournamentMatchRepository).save(scheduledMatch);
    }

    @Test
    void submitResultAllowsAmericanoDinamicoMatches() {
        Tournament tournament = Tournament.builder()
                .id(311L)
                .name("Americano Dinamico QA")
                .format(TournamentFormat.AMERICANO)
                .americanoType(TournamentAmericanoType.DYNAMIC)
                .status(TournamentStatus.IN_PROGRESS)
                .startDate(LocalDate.of(2026, 4, 10))
                .matchesPerParticipant(3)
                .build();

        PlayerProfile submitter = player(41L, "Dynamic One");
        TournamentEntry teamOne = entry(711L, tournament, null, "Dynamic One & Dynamic Two", submitter, player(42L, "Dynamic Two"));
        TournamentEntry teamTwo = entry(712L, tournament, null, "Dynamic Three & Dynamic Four", player(43L, "Dynamic Three"), player(44L, "Dynamic Four"));
        teamOne.setEntryKind(TournamentEntryKind.GENERATED_MATCH_PAIR);
        teamTwo.setEntryKind(TournamentEntryKind.GENERATED_MATCH_PAIR);

        TournamentMatch scheduledMatch = TournamentMatch.builder()
                .id(811L)
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .phase(TournamentMatchPhase.AMERICANO_STAGE)
                .status(TournamentMatchStatus.SCHEDULED)
                .roundNumber(1)
                .roundLabel("Ronda 1")
                .scheduledAt(Instant.parse("2026-03-01T18:00:00Z"))
                .courtName("Cancha 1")
                .build();

        SubmitTournamentMatchResultRequest request = new SubmitTournamentMatchResultRequest(
                MatchWinnerTeam.TEAM_TWO,
                List.of(
                        new TournamentMatchScoreSetRequest(4, 6),
                        new TournamentMatchScoreSetRequest(3, 6)
                )
        );

        when(tournamentService.getTournamentEntity(311L)).thenReturn(tournament);
        when(playerProfileResolverService.getOrCreateByUserEmail("dynamic@example.com")).thenReturn(submitter);
        when(tournamentMatchRepository.findByIdAndTournamentId(811L, 311L)).thenReturn(Optional.of(scheduledMatch));
        when(tournamentMatchResultRepository.findByTournamentMatchId(811L)).thenReturn(Optional.empty());
        when(tournamentMatchResultRepository.save(any(TournamentMatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentMatchRepository.save(any(TournamentMatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentMatchResultResponse response = tournamentMatchService.submitResult("dynamic@example.com", 311L, 811L, request);

        assertEquals(TournamentMatchResultStatus.PENDING, response.status());
        assertEquals(TournamentMatchStatus.RESULT_PENDING, scheduledMatch.getStatus());
        verify(tournamentMatchResultRepository).save(any(TournamentMatchResult.class));
        verify(tournamentMatchRepository).save(scheduledMatch);
    }

    private TournamentStandingsEntryResponse standingsEntry(int position, TournamentEntry entry, String teamName) {
        return new TournamentStandingsEntryResponse(
                position,
                entry.getId(),
                teamName,
                List.of(
                        new TournamentEntryMemberResponse(
                                entry.getPrimaryPlayerProfile().getId(),
                                entry.getPrimaryPlayerProfile().getUser().getId(),
                                entry.getPrimaryPlayerProfile().getFullName()
                        )
                ),
                3,
                1,
                1,
                0,
                2,
                0,
                2,
                12,
                7,
                5
        );
    }

    private TournamentEntry entry(
            Long id,
            Tournament tournament,
            String groupLabel,
            String teamName,
            PlayerProfile primary,
            PlayerProfile secondary
    ) {
        return TournamentEntry.builder()
                .id(id)
                .tournament(tournament)
                .primaryPlayerProfile(primary)
                .secondaryPlayerProfile(secondary)
                .teamName(teamName)
                .groupLabel(groupLabel)
                .entryKind(TournamentEntryKind.REGISTERED)
                .status(TournamentEntryStatus.CONFIRMED)
                .timePreferences(List.of())
                .createdAt(Instant.parse("2026-04-01T12:00:00Z"))
                .build();
    }

    private PlayerProfile player(Long id, String fullName) {
        User user = User.builder()
                .id(id + 1000)
                .email(fullName.toLowerCase().replace(" ", "") + "@example.com")
                .passwordHash("hashed")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();

        return PlayerProfile.builder()
                .id(id)
                .user(user)
                .fullName(fullName)
                .build();
    }
}
