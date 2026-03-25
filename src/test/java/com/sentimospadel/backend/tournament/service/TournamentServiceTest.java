package com.sentimospadel.backend.tournament.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.tournament.dto.CreateTournamentRequest;
import com.sentimospadel.backend.tournament.dto.LaunchTournamentRequest;
import com.sentimospadel.backend.tournament.dto.TournamentEntryMemberResponse;
import com.sentimospadel.backend.tournament.dto.TournamentEntryResponse;
import com.sentimospadel.backend.tournament.dto.TournamentResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
import com.sentimospadel.backend.tournament.repository.TournamentEntryRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentRepository;
import com.sentimospadel.backend.user.entity.User;
import java.math.BigDecimal;
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
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentEntryRepository tournamentEntryRepository;

    @Mock
    private TournamentMatchRepository tournamentMatchRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    @Mock
    private TournamentMapper tournamentMapper;

    @InjectMocks
    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        lenient().when(tournamentMapper.toTournamentResponse(any(Tournament.class), anyList(), anyLong()))
                .thenAnswer(invocation -> {
                    Tournament tournament = invocation.getArgument(0);
                    @SuppressWarnings("unchecked")
                    List<TournamentEntry> entries = invocation.getArgument(1);
                    long generatedMatchesCount = invocation.getArgument(2);
                    return new TournamentResponse(
                            tournament.getId(),
                            tournament.getCreatedBy().getId(),
                            tournament.getName(),
                            tournament.getDescription(),
                            tournament.getClub() == null ? null : tournament.getClub().getId(),
                            tournament.getCity(),
                            tournament.getStartDate(),
                            tournament.getEndDate(),
                            tournament.getStatus(),
                            tournament.getFormat(),
                            tournament.getAmericanoType(),
                            tournament.isOpenEnrollment(),
                            tournament.isCompetitive(),
                            tournament.getMaxEntries(),
                            entries.size(),
                            entries.stream().mapToInt(this::memberCount).sum(),
                            tournament.getAvailableCourts(),
                            tournament.getNumberOfGroups(),
                            tournament.getLeagueRounds(),
                            tournament.getStandingsTiebreak(),
                            tournament.getCourtNames() == null ? List.of() : tournament.getCourtNames(),
                            tournament.getLaunchedAt(),
                            false,
                            Math.toIntExact(generatedMatchesCount),
                            entries.stream().map(this::toEntryResponse).toList(),
                            tournament.getCreatedAt(),
                            tournament.getUpdatedAt()
                    );
                });
    }

    @Test
    void createTournamentCreatesOpenLeagueTournament() {
        PlayerProfile creator = playerProfile(10L, "creator@example.com", "Creator");
        Club club = Club.builder().id(5L).name("Top Padel").city("Montevideo").build();
        CreateTournamentRequest request = new CreateTournamentRequest(
                "Open Montevideo",
                "Primer torneo social",
                5L,
                "Montevideo",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 20),
                TournamentFormat.LEAGUE,
                null,
                8,
                true,
                true,
                2,
                TournamentStandingsTiebreak.GAMES_DIFFERENCE,
                null,
                List.of(),
                List.of()
        );

        when(playerProfileResolverService.getOrCreateByUserEmail("creator@example.com")).thenReturn(creator);
        when(clubRepository.findById(5L)).thenReturn(Optional.of(club));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            Tournament saved = Tournament.builder()
                    .id(100L)
                    .createdBy(tournament.getCreatedBy())
                    .name(tournament.getName())
                    .description(tournament.getDescription())
                    .format(tournament.getFormat())
                    .americanoType(tournament.getAmericanoType())
                    .club(tournament.getClub())
                    .city(tournament.getCity())
                    .startDate(tournament.getStartDate())
                    .endDate(tournament.getEndDate())
                    .status(tournament.getStatus())
                    .maxEntries(tournament.getMaxEntries())
                    .openEnrollment(tournament.isOpenEnrollment())
                    .competitive(tournament.isCompetitive())
                    .leagueRounds(tournament.getLeagueRounds())
                    .pointsForWin(tournament.getPointsForWin())
                    .pointsForTiebreakLoss(tournament.getPointsForTiebreakLoss())
                    .pointsForLoss(tournament.getPointsForLoss())
                    .standingsTiebreak(tournament.getStandingsTiebreak())
                    .courtNames(tournament.getCourtNames())
                    .build();
            saved.setCreatedAt(Instant.parse("2026-03-24T12:00:00Z"));
            saved.setUpdatedAt(Instant.parse("2026-03-24T12:00:00Z"));
            return saved;
        });
        when(tournamentEntryRepository.findAllByTournamentIdOrderByCreatedAtAsc(100L)).thenReturn(List.of());
        when(tournamentMatchRepository.countByTournamentId(100L)).thenReturn(0L);

        TournamentResponse response = tournamentService.createTournament("creator@example.com", request);

        assertEquals(100L, response.id());
        assertEquals(TournamentStatus.OPEN, response.status());
        assertEquals(TournamentFormat.LEAGUE, response.format());
        assertEquals(8, response.maxEntries());
        assertEquals(2, response.leagueRounds());
    }

    @Test
    void joinTournamentCreatesPendingEntryForLeague() {
        PlayerProfile creator = playerProfile(10L, "creator@example.com", "Creator");
        PlayerProfile joiner = playerProfile(11L, "joiner@example.com", "Joiner");
        Tournament tournament = tournament(200L, creator, TournamentStatus.OPEN, TournamentFormat.LEAGUE, 8);
        TournamentEntry savedEntry = TournamentEntry.builder()
                .id(301L)
                .tournament(tournament)
                .primaryPlayerProfile(joiner)
                .createdBy(joiner)
                .status(TournamentEntryStatus.PENDING)
                .createdAt(Instant.parse("2026-03-24T13:00:00Z"))
                .timePreferences(List.of())
                .build();

        when(playerProfileResolverService.getOrCreateByUserEmail("joiner@example.com")).thenReturn(joiner);
        when(tournamentRepository.findById(200L)).thenReturn(Optional.of(tournament));
        when(tournamentEntryRepository.findAllByTournamentIdOrderByCreatedAtAsc(200L))
                .thenReturn(List.of(), List.of(savedEntry));
        when(tournamentEntryRepository.save(any(TournamentEntry.class))).thenReturn(savedEntry);
        when(tournamentMatchRepository.countByTournamentId(200L)).thenReturn(0L);

        TournamentResponse response = tournamentService.joinTournament("joiner@example.com", 200L);

        assertEquals(1, response.currentEntriesCount());
        assertEquals(TournamentEntryStatus.PENDING, response.entries().getFirst().status());
        verify(tournamentEntryRepository).save(any(TournamentEntry.class));
    }

    @Test
    void joinTournamentRejectsDuplicateEntryAcrossTournamentTeams() {
        PlayerProfile creator = playerProfile(10L, "creator@example.com", "Creator");
        PlayerProfile joiner = playerProfile(11L, "joiner@example.com", "Joiner");
        Tournament tournament = tournament(201L, creator, TournamentStatus.OPEN, TournamentFormat.LEAGUE, 8);
        TournamentEntry existingEntry = TournamentEntry.builder()
                .id(302L)
                .tournament(tournament)
                .primaryPlayerProfile(joiner)
                .createdBy(joiner)
                .status(TournamentEntryStatus.PENDING)
                .createdAt(Instant.parse("2026-03-24T13:00:00Z"))
                .timePreferences(List.of())
                .build();

        when(playerProfileResolverService.getOrCreateByUserEmail("joiner@example.com")).thenReturn(joiner);
        when(tournamentRepository.findById(201L)).thenReturn(Optional.of(tournament));
        when(tournamentEntryRepository.findAllByTournamentIdOrderByCreatedAtAsc(201L)).thenReturn(List.of(existingEntry));

        assertThrows(ConflictException.class, () -> tournamentService.joinTournament("joiner@example.com", 201L));
    }

    @Test
    void leaveTournamentRemovesSinglePlayerEntry() {
        PlayerProfile creator = playerProfile(10L, "creator@example.com", "Creator");
        PlayerProfile player = playerProfile(11L, "joiner@example.com", "Joiner");
        Tournament tournament = tournament(203L, creator, TournamentStatus.OPEN, TournamentFormat.LEAGUE, 8);
        TournamentEntry entry = TournamentEntry.builder()
                .id(300L)
                .tournament(tournament)
                .primaryPlayerProfile(player)
                .createdBy(player)
                .status(TournamentEntryStatus.PENDING)
                .createdAt(Instant.parse("2026-03-24T13:00:00Z"))
                .timePreferences(List.of())
                .build();

        when(playerProfileResolverService.getOrCreateByUserEmail("joiner@example.com")).thenReturn(player);
        when(tournamentRepository.findById(203L)).thenReturn(Optional.of(tournament));
        when(tournamentEntryRepository.findAllByTournamentIdOrderByCreatedAtAsc(203L))
                .thenReturn(List.of(entry), List.of());
        when(tournamentMatchRepository.countByTournamentId(203L)).thenReturn(0L);

        TournamentResponse response = tournamentService.leaveTournament("joiner@example.com", 203L);

        assertEquals(0, response.currentEntriesCount());
        verify(tournamentEntryRepository).delete(entry);
    }

    @Test
    void launchTournamentGeneratesLeagueMatches() {
        PlayerProfile creator = playerProfile(10L, "creator@example.com", "Creator");
        Tournament tournament = tournament(204L, creator, TournamentStatus.OPEN, TournamentFormat.LEAGUE, 4);
        tournament.setEndDate(LocalDate.of(2026, 4, 20));

        List<TournamentEntry> entries = List.of(
                confirmedEntry(401L, tournament, playerProfile(20L, "a1@example.com", "A1"), playerProfile(21L, "a2@example.com", "A2"), "Team A"),
                confirmedEntry(402L, tournament, playerProfile(22L, "b1@example.com", "B1"), playerProfile(23L, "b2@example.com", "B2"), "Team B"),
                confirmedEntry(403L, tournament, playerProfile(24L, "c1@example.com", "C1"), playerProfile(25L, "c2@example.com", "C2"), "Team C"),
                confirmedEntry(404L, tournament, playerProfile(26L, "d1@example.com", "D1"), playerProfile(27L, "d2@example.com", "D2"), "Team D")
        );

        when(playerProfileResolverService.getOrCreateByUserEmail("creator@example.com")).thenReturn(creator);
        when(tournamentRepository.findById(204L)).thenReturn(Optional.of(tournament));
        when(tournamentEntryRepository.findAllByTournamentIdOrderByCreatedAtAsc(204L)).thenReturn(entries);
        when(tournamentMatchRepository.existsByTournamentId(204L)).thenReturn(false);
        when(tournamentMatchRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentMatchRepository.countByTournamentId(204L)).thenReturn(12L);

        TournamentResponse response = tournamentService.launchTournament(
                "creator@example.com",
                204L,
                new LaunchTournamentRequest(2, 1, 2, List.of("Cancha 1", "Cancha 2"))
        );

        assertEquals(TournamentStatus.IN_PROGRESS, response.status());
        assertEquals(12, response.generatedMatchesCount());

        ArgumentCaptor<List<TournamentMatch>> matchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(tournamentMatchRepository).saveAll(matchesCaptor.capture());
        assertEquals(12, matchesCaptor.getValue().size());
    }

    private TournamentEntryResponse toEntryResponse(TournamentEntry entry) {
        return new TournamentEntryResponse(
                entry.getId(),
                entry.getTeamName(),
                entry.getStatus(),
                entry.getTimePreferences(),
                List.of(
                        new TournamentEntryMemberResponse(
                                entry.getPrimaryPlayerProfile().getId(),
                                entry.getPrimaryPlayerProfile().getUser().getId(),
                                entry.getPrimaryPlayerProfile().getFullName()
                        )
                ),
                entry.getCreatedAt()
        );
    }

    private int memberCount(TournamentEntry entry) {
        return entry.getSecondaryPlayerProfile() == null ? 1 : 2;
    }

    private TournamentEntry confirmedEntry(Long id, Tournament tournament, PlayerProfile p1, PlayerProfile p2, String teamName) {
        return TournamentEntry.builder()
                .id(id)
                .tournament(tournament)
                .primaryPlayerProfile(p1)
                .secondaryPlayerProfile(p2)
                .createdBy(p1)
                .teamName(teamName)
                .status(TournamentEntryStatus.CONFIRMED)
                .timePreferences(List.of())
                .createdAt(Instant.parse("2026-03-24T12:10:00Z"))
                .build();
    }

    private Tournament tournament(Long id, PlayerProfile creator, TournamentStatus status, TournamentFormat format, Integer maxEntries) {
        Tournament tournament = Tournament.builder()
                .id(id)
                .createdBy(creator)
                .name("Open Montevideo")
                .startDate(LocalDate.of(2026, 4, 15))
                .endDate(LocalDate.of(2026, 4, 20))
                .status(status)
                .format(format)
                .openEnrollment(true)
                .competitive(true)
                .leagueRounds(2)
                .pointsForWin(3)
                .pointsForTiebreakLoss(1)
                .pointsForLoss(0)
                .standingsTiebreak(TournamentStandingsTiebreak.GAMES_DIFFERENCE)
                .maxEntries(maxEntries)
                .build();
        tournament.setCreatedAt(Instant.parse("2026-03-24T12:00:00Z"));
        tournament.setUpdatedAt(Instant.parse("2026-03-24T12:00:00Z"));
        return tournament;
    }

    private PlayerProfile playerProfile(Long id, String email, String fullName) {
        return PlayerProfile.builder()
                .id(id)
                .user(User.builder().id(id + 100).email(email).build())
                .fullName(fullName)
                .currentRating(new BigDecimal("4.50"))
                .provisional(false)
                .matchesPlayed(0)
                .ratedMatchesCount(0)
                .build();
    }
}
