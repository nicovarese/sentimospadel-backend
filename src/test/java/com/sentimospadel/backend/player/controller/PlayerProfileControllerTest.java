package com.sentimospadel.backend.player.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.match.dto.MatchParticipantResponse;
import com.sentimospadel.backend.match.dto.MatchResultSummaryResponse;
import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.dto.PlayerMatchHistoryEntryResponse;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.enums.PlayerMatchHistoryScope;
import com.sentimospadel.backend.match.service.PlayerMatchHistoryService;
import com.sentimospadel.backend.player.dto.PlayerProfileResponse;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.player.service.PlayerProfileService;
import com.sentimospadel.backend.rating.dto.RatingHistoryEntryResponse;
import com.sentimospadel.backend.rating.dto.RatingHistoryMatchSummaryResponse;
import com.sentimospadel.backend.rating.service.PlayerRatingHistoryService;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

@ExtendWith(MockitoExtension.class)
class PlayerProfileControllerTest {

    @Mock
    private PlayerProfileService playerProfileService;

    @Mock
    private PlayerRatingHistoryService playerRatingHistoryService;

    @Mock
    private PlayerMatchHistoryService playerMatchHistoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PlayerProfileController(
                        playerProfileService,
                        playerRatingHistoryService,
                        playerMatchHistoryService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new MeAuthenticationRequiredFilter())
                .build();
    }

    @Test
    void myProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/players/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myRatingHistoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/players/me/rating-history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myMatchesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/players/me/matches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicPlayerRatingHistoryReturnsNewestFirst() throws Exception {
        when(playerRatingHistoryService.getRatingHistoryForPlayer(10L)).thenReturn(List.of(
                new RatingHistoryEntryResponse(
                        100L,
                        20L,
                        new BigDecimal("4.80"),
                        new BigDecimal("0.06"),
                        new BigDecimal("4.86"),
                        Instant.parse("2026-03-17T18:00:00Z"),
                        new RatingHistoryMatchSummaryResponse(
                                20L,
                                MatchStatus.COMPLETED,
                                Instant.parse("2026-03-17T17:00:00Z"),
                                MatchWinnerTeam.TEAM_ONE,
                                new MatchScoreResponse(2, 1)
                        )
                )
        ));

        mockMvc.perform(get("/api/players/10/rating-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].oldRating").value(4.80))
                .andExpect(jsonPath("$[0].match.matchStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[0].match.score.teamOneScore").value(2));
    }

    @Test
    void myRatingHistoryReturnsAuthenticatedPlayerEntries() throws Exception {
        when(playerRatingHistoryService.getMyRatingHistory("player@example.com")).thenReturn(List.of(
                new RatingHistoryEntryResponse(
                        101L,
                        21L,
                        new BigDecimal("4.86"),
                        new BigDecimal("-0.04"),
                        new BigDecimal("4.82"),
                        Instant.parse("2026-03-18T18:00:00Z"),
                        null
                )
        ));

        mockMvc.perform(get("/api/players/me/rating-history")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].newRating").value(4.82));
    }

    @Test
    void myProfileReturnsAuthenticatedPlayerProfile() throws Exception {
        when(playerProfileService.getMyPlayerProfile("player@example.com")).thenReturn(
                new PlayerProfileResponse(
                        10L,
                        100L,
                        "Player One",
                        null,
                        null,
                        null,
                        "Montevideo",
                        null,
                        new BigDecimal("4.82"),
                        UruguayCategory.TERCERA,
                        false,
                        12,
                        7,
                        true,
                        Instant.parse("2026-03-17T10:00:00Z"),
                        new BigDecimal("4.60"),
                        UruguayCategory.TERCERA,
                        false,
                        ClubVerificationStatus.NOT_REQUIRED,
                        Instant.parse("2026-03-16T10:00:00Z"),
                        Instant.parse("2026-03-18T10:00:00Z")
                )
        );

        mockMvc.perform(get("/api/players/me")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.fullName").value("Player One"))
                .andExpect(jsonPath("$.currentRating").value(4.82))
                .andExpect(jsonPath("$.currentCategory").value("TERCERA"));
    }

    @Test
    void myMatchesReturnsAuthenticatedPlayerMatchHistory() throws Exception {
        when(playerMatchHistoryService.getMyMatches("player@example.com", null)).thenReturn(List.of(
                new PlayerMatchHistoryEntryResponse(
                        55L,
                        MatchStatus.COMPLETED,
                        Instant.parse("2026-03-20T20:00:00Z"),
                        null,
                        "Rambla",
                        "Partido social",
                        4,
                        List.of(
                                new MatchParticipantResponse(10L, 100L, "Player One", MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-20T19:00:00Z")),
                                new MatchParticipantResponse(11L, 101L, "Player Two", MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-20T19:01:00Z"))
                        ),
                        true,
                        new MatchResultSummaryResponse(
                                MatchResultStatus.CONFIRMED,
                                MatchWinnerTeam.TEAM_ONE,
                                new MatchScoreResponse(2, 0),
                                Instant.parse("2026-03-20T22:00:00Z"),
                                10L,
                                12L,
                                Instant.parse("2026-03-20T22:05:00Z"),
                                null,
                                null,
                                null
                        ),
                        true,
                        MatchParticipantTeam.TEAM_ONE,
                        true,
                        Instant.parse("2026-03-20T18:00:00Z"),
                        Instant.parse("2026-03-20T22:05:00Z")
                )
        ));

        mockMvc.perform(get("/api/players/me/matches")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(55))
                .andExpect(jsonPath("$[0].authenticatedPlayerIsParticipant").value(true))
                .andExpect(jsonPath("$[0].authenticatedPlayerTeam").value("TEAM_ONE"))
                .andExpect(jsonPath("$[0].authenticatedPlayerWon").value(true))
                .andExpect(jsonPath("$[0].result.status").value("CONFIRMED"));
    }

    @Test
    void myMatchesAcceptsScopeFilter() throws Exception {
        when(playerMatchHistoryService.getMyMatches("player@example.com", PlayerMatchHistoryScope.UPCOMING))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/players/me/matches")
                        .queryParam("scope", "upcoming")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk());

        verify(playerMatchHistoryService).getMyMatches("player@example.com", PlayerMatchHistoryScope.UPCOMING);
    }

    @Test
    void myMatchesRejectsInvalidScope() throws Exception {
        mockMvc.perform(get("/api/players/me/matches")
                        .queryParam("scope", "later")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid match scope. Supported values: upcoming, completed, cancelled, pending_result"
                ));
    }

    private static class MeAuthenticationRequiredFilter extends OncePerRequestFilter {

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return !uri.equals("/api/players/me") && !uri.startsWith("/api/players/me/");
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
