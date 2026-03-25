package com.sentimospadel.backend.tournament.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import com.sentimospadel.backend.tournament.dto.CreateTournamentRequest;
import com.sentimospadel.backend.tournament.dto.TournamentEntryMemberResponse;
import com.sentimospadel.backend.tournament.dto.TournamentEntryResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchTeamResponse;
import com.sentimospadel.backend.tournament.dto.TournamentResponse;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
import com.sentimospadel.backend.tournament.service.TournamentMatchService;
import com.sentimospadel.backend.tournament.service.TournamentService;
import com.sentimospadel.backend.tournament.service.TournamentStandingsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

@ExtendWith(MockitoExtension.class)
class TournamentControllerTest {

    @Mock
    private TournamentService tournamentService;

    @Mock
    private TournamentMatchService tournamentMatchService;

    @Mock
    private TournamentStandingsService tournamentStandingsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TournamentController(
                        tournamentService,
                        tournamentMatchService,
                        tournamentStandingsService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationRequiredFilter())
                .build();
    }

    @Test
    void createTournamentRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Liga Montevideo\",\"startDate\":\"2026-04-15\",\"format\":\"LEAGUE\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTournamentReturnsCreatedResponse() throws Exception {
        when(tournamentService.createTournament(eq("player@example.com"), any(CreateTournamentRequest.class)))
                .thenReturn(buildResponse());

        mockMvc.perform(post("/api/tournaments")
                        .principal(new TestingAuthenticationToken("player@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Liga Montevideo",
                                  "description": "Liga doble ronda",
                                  "startDate": "2026-04-15",
                                  "endDate": "2026-04-20",
                                  "format": "LEAGUE",
                                  "maxEntries": 8,
                                  "openEnrollment": true,
                                  "competitive": true,
                                  "leagueRounds": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/tournaments/1"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.format").value("LEAGUE"))
                .andExpect(jsonPath("$.currentEntriesCount").value(1));
    }

    @Test
    void getTournamentsIsPublic() throws Exception {
        when(tournamentService.getTournaments()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].entries[0].teamName").value("Dupla Uno"));
    }

    @Test
    void getTournamentMatchesIsPublic() throws Exception {
        when(tournamentMatchService.getTournamentMatches(1L)).thenReturn(List.of(
                new TournamentMatchResponse(
                        10L,
                        1L,
                        TournamentMatchPhase.LEAGUE_STAGE,
                        TournamentMatchStatus.SCHEDULED,
                        1,
                        1,
                        "Jornada 1 - Vuelta 1",
                        Instant.parse("2026-04-15T18:00:00Z"),
                        "Cancha 1",
                        new TournamentMatchTeamResponse(100L, "Dupla Uno", List.of()),
                        new TournamentMatchTeamResponse(101L, "Dupla Dos", List.of()),
                        false,
                        null,
                        Instant.parse("2026-03-24T12:00:00Z"),
                        Instant.parse("2026-03-24T12:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/tournaments/1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roundLabel").value("Jornada 1 - Vuelta 1"))
                .andExpect(jsonPath("$[0].teamOne.teamName").value("Dupla Uno"));
    }

    @Test
    void syncEntriesRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/tournaments/1/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    private TournamentResponse buildResponse() {
        return new TournamentResponse(
                1L,
                10L,
                "Liga Montevideo",
                "Liga doble ronda",
                null,
                "Montevideo",
                LocalDate.of(2026, 4, 15),
                LocalDate.of(2026, 4, 20),
                TournamentStatus.OPEN,
                TournamentFormat.LEAGUE,
                null,
                true,
                true,
                8,
                1,
                2,
                null,
                null,
                2,
                TournamentStandingsTiebreak.GAMES_DIFFERENCE,
                List.of(),
                null,
                false,
                0,
                List.of(new TournamentEntryResponse(
                        100L,
                        "Dupla Uno",
                        TournamentEntryStatus.CONFIRMED,
                        List.of(),
                        List.of(
                                new TournamentEntryMemberResponse(10L, 110L, "Player One"),
                                new TournamentEntryMemberResponse(11L, 111L, "Player Two")
                        ),
                        Instant.parse("2026-03-24T12:10:00Z")
                )),
                Instant.parse("2026-03-24T12:00:00Z"),
                Instant.parse("2026-03-24T12:00:00Z")
        );
    }

    private static class AuthenticationRequiredFilter extends OncePerRequestFilter {

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return "GET".equalsIgnoreCase(request.getMethod());
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            if (request.getUserPrincipal() == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            filterChain.doFilter(request, response);
        }
    }
}
