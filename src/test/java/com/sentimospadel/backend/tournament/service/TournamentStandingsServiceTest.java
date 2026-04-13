package com.sentimospadel.backend.tournament.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentEntryKind;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
import com.sentimospadel.backend.tournament.repository.TournamentEntryRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentStandingsServiceTest {

    @Mock
    private TournamentService tournamentService;

    @Mock
    private TournamentEntryRepository tournamentEntryRepository;

    @Mock
    private TournamentMatchRepository tournamentMatchRepository;

    @Mock
    private TournamentMatchResultRepository tournamentMatchResultRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @InjectMocks
    private TournamentStandingsService tournamentStandingsService;

    @Test
    void getStandingsSupportsConfirmedResultsWithOnlyTwoSets() {
        Tournament tournament = Tournament.builder()
                .id(10L)
                .name("Liga QA")
                .format(TournamentFormat.LEAGUE)
                .standingsTiebreak(TournamentStandingsTiebreak.GAMES_DIFFERENCE)
                .pointsForWin(3)
                .pointsForTiebreakLoss(1)
                .pointsForLoss(0)
                .startDate(LocalDate.of(2026, 4, 1))
                .build();

        TournamentEntry teamOne = entry(101L, tournament, "Lobos QA", player(1L, "Nicolas"));
        TournamentEntry teamTwo = entry(102L, tournament, "Sur QA", player(2L, "Ana"));

        TournamentMatch match = TournamentMatch.builder()
                .id(201L)
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .phase(TournamentMatchPhase.LEAGUE_STAGE)
                .roundNumber(1)
                .roundLabel("QA Jornada 1")
                .scheduledAt(Instant.parse("2026-04-02T18:00:00Z"))
                .build();

        TournamentMatchResult confirmedResult = TournamentMatchResult.builder()
                .id(301L)
                .tournamentMatch(match)
                .submittedBy(teamOne.getPrimaryPlayerProfile())
                .status(com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus.CONFIRMED)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .setOneTeamOneGames(6)
                .setOneTeamTwoGames(4)
                .setTwoTeamOneGames(6)
                .setTwoTeamTwoGames(3)
                .submittedAt(Instant.parse("2026-04-02T20:00:00Z"))
                .confirmedAt(Instant.parse("2026-04-02T20:30:00Z"))
                .build();

        when(tournamentService.getTournamentEntity(10L)).thenReturn(tournament);
        when(tournamentEntryRepository.findAllByTournamentIdAndEntryKindOrderByCreatedAtAsc(10L, TournamentEntryKind.REGISTERED))
                .thenReturn(List.of(teamOne, teamTwo));
        when(tournamentMatchResultRepository.findAllByTournamentMatchTournamentId(10L))
                .thenReturn(List.of(confirmedResult));
        when(tournamentMapper.displayTeamName(any(TournamentEntry.class)))
                .thenAnswer(invocation -> invocation.<TournamentEntry>getArgument(0).getTeamName());
        when(tournamentMapper.toMemberResponses(any(TournamentEntry.class)))
                .thenReturn(List.of());

        TournamentStandingsResponse response = tournamentStandingsService.getStandings(10L);

        assertNotNull(response);
        assertEquals(2, response.standings().size());
        assertEquals("Lobos QA", response.standings().get(0).teamName());
        assertEquals(3, response.standings().get(0).points());
        assertEquals(1, response.standings().get(0).played());
        assertEquals("Sur QA", response.standings().get(1).teamName());
        assertEquals(0, response.standings().get(1).points());
        assertEquals(1, response.standings().get(1).played());
    }

    private TournamentEntry entry(Long id, Tournament tournament, String teamName, PlayerProfile primary) {
        return TournamentEntry.builder()
                .id(id)
                .tournament(tournament)
                .primaryPlayerProfile(primary)
                .teamName(teamName)
                .entryKind(TournamentEntryKind.REGISTERED)
                .status(TournamentEntryStatus.CONFIRMED)
                .createdAt(Instant.parse("2026-04-01T12:00:00Z"))
                .build();
    }

    private PlayerProfile player(Long id, String fullName) {
        User user = User.builder()
                .id(id + 1000)
                .email(fullName.toLowerCase() + "@example.com")
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
