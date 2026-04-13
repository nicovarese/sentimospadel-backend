package com.sentimospadel.backend.rating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TournamentRatingApplicationServiceTest {

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private PlayerRatingHistoryRepository playerRatingHistoryRepository;

    @Mock
    private TournamentMatchResultRepository tournamentMatchResultRepository;

    private TournamentRatingApplicationService tournamentRatingApplicationService;

    @BeforeEach
    void setUp() {
        tournamentRatingApplicationService = new TournamentRatingApplicationService(
                playerProfileRepository,
                playerRatingHistoryRepository,
                tournamentMatchResultRepository,
                new RatingCalculationService()
        );
    }

    @Test
    void competitiveEliminationAppliesOfficialRatingOnce() {
        Tournament tournament = Tournament.builder()
                .name("Eliminatoria Pro")
                .format(TournamentFormat.ELIMINATION)
                .competitive(true)
                .status(com.sentimospadel.backend.tournament.enums.TournamentStatus.IN_PROGRESS)
                .startDate(LocalDate.of(2026, 4, 10))
                .build();
        ReflectionTestUtils.setField(tournament, "id", 80L);

        TournamentEntry teamOne = confirmedEntry(10L, tournament, profile(1L, "A1", "4.80", 0), profile(2L, "A2", "4.60", 2), "Team A");
        TournamentEntry teamTwo = confirmedEntry(11L, tournament, profile(3L, "B1", "4.90", 4), profile(4L, "B2", "4.70", 6), "Team B");

        TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .phase(TournamentMatchPhase.GROUP_STAGE)
                .status(TournamentMatchStatus.COMPLETED)
                .roundNumber(1)
                .roundLabel("Fase de Grupos - Grupo A")
                .scheduledAt(Instant.parse("2026-04-10T18:00:00Z"))
                .courtName("Cancha 1")
                .build();
        ReflectionTestUtils.setField(match, "id", 90L);

        TournamentMatchResult result = TournamentMatchResult.builder()
                .tournamentMatch(match)
                .submittedBy(teamOne.getPrimaryPlayerProfile())
                .status(TournamentMatchResultStatus.CONFIRMED)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .setOneTeamOneGames(6)
                .setOneTeamTwoGames(4)
                .setTwoTeamOneGames(6)
                .setTwoTeamTwoGames(3)
                .confirmedAt(Instant.parse("2026-04-10T20:00:00Z"))
                .build();

        when(playerRatingHistoryRepository.countByTournamentMatchId(90L)).thenReturn(0L);
        when(tournamentMatchResultRepository.save(any(TournamentMatchResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean applied = tournamentRatingApplicationService.applyConfirmedCompetitiveResultIfNeeded(result);

        assertTrue(applied);
        assertTrue(result.isRatingApplied());
        assertEquals(1, teamOne.getPrimaryPlayerProfile().getRatedMatchesCount());
        assertEquals(1, teamOne.getPrimaryPlayerProfile().getMatchesPlayed());

        ArgumentCaptor<List<PlayerRatingHistory>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingHistoryRepository).saveAll(historyCaptor.capture());
        assertEquals(4, historyCaptor.getValue().size());
        assertTrue(historyCaptor.getValue().stream().allMatch(history -> history.getTournamentMatch() == match));
    }

    @Test
    void recreationalEliminationDoesNotApplyOfficialRating() {
        Tournament tournament = Tournament.builder()
                .name("Eliminatoria Recreativa")
                .format(TournamentFormat.ELIMINATION)
                .competitive(false)
                .status(com.sentimospadel.backend.tournament.enums.TournamentStatus.IN_PROGRESS)
                .startDate(LocalDate.of(2026, 4, 10))
                .build();

        TournamentEntry teamOne = confirmedEntry(10L, tournament, profile(1L, "A1", "4.80", 0), profile(2L, "A2", "4.60", 2), "Team A");
        TournamentEntry teamTwo = confirmedEntry(11L, tournament, profile(3L, "B1", "4.90", 4), profile(4L, "B2", "4.70", 6), "Team B");

        TournamentMatch match = TournamentMatch.builder()
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .phase(TournamentMatchPhase.GROUP_STAGE)
                .status(TournamentMatchStatus.COMPLETED)
                .roundNumber(1)
                .roundLabel("Fase de Grupos - Grupo A")
                .scheduledAt(Instant.parse("2026-04-10T18:00:00Z"))
                .courtName("Cancha 1")
                .build();
        ReflectionTestUtils.setField(match, "id", 91L);

        TournamentMatchResult result = TournamentMatchResult.builder()
                .tournamentMatch(match)
                .submittedBy(teamOne.getPrimaryPlayerProfile())
                .status(TournamentMatchResultStatus.CONFIRMED)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .setOneTeamOneGames(6)
                .setOneTeamTwoGames(4)
                .setTwoTeamOneGames(6)
                .setTwoTeamTwoGames(3)
                .confirmedAt(Instant.parse("2026-04-10T20:00:00Z"))
                .build();

        boolean applied = tournamentRatingApplicationService.applyConfirmedCompetitiveResultIfNeeded(result);

        assertFalse(applied);
        verify(playerProfileRepository, never()).saveAll(any());
        verify(playerRatingHistoryRepository, never()).saveAll(any());
    }

    private TournamentEntry confirmedEntry(
            Long id,
            Tournament tournament,
            PlayerProfile primary,
            PlayerProfile secondary,
            String teamName
    ) {
        TournamentEntry entry = TournamentEntry.builder()
                .tournament(tournament)
                .primaryPlayerProfile(primary)
                .secondaryPlayerProfile(secondary)
                .teamName(teamName)
                .status(TournamentEntryStatus.CONFIRMED)
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }

    private PlayerProfile profile(Long id, String fullName, String rating, int ratedMatchesCount) {
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
