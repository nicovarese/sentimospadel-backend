package com.sentimospadel.backend.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.match.dto.AssignMatchTeamsRequest;
import com.sentimospadel.backend.match.dto.CreateMatchRequest;
import com.sentimospadel.backend.match.dto.MatchInviteLinkResponse;
import com.sentimospadel.backend.match.dto.MatchInvitePreviewResponse;
import com.sentimospadel.backend.match.dto.MatchParticipantResponse;
import com.sentimospadel.backend.match.dto.MatchResultResponse;
import com.sentimospadel.backend.match.dto.MatchResultSummaryResponse;
import com.sentimospadel.backend.match.dto.MatchResponse;
import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.dto.MatchTeamAssignmentRequest;
import com.sentimospadel.backend.match.dto.RejectMatchResultRequest;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.service.MatchInviteService;
import com.sentimospadel.backend.match.service.MatchService;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
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
class MatchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MatchService matchService;

    @Mock
    private MatchInviteService matchInviteService;

    @BeforeEach
    void setUp() {
        MatchController controller = new MatchController(matchService, matchInviteService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationRequiredFilter())
                .build();
    }

    @Test
    void createMatchRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"2026-03-20T20:00:00Z\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMatchReturnsCreatedResponse() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest(
                Instant.parse("2026-03-20T20:00:00Z"),
                null,
                "Rambla",
                "Partido social"
        );

        MatchResponse response = new MatchResponse(
                1L,
                10L,
                MatchStatus.OPEN,
                Instant.parse("2026-03-20T20:00:00Z"),
                null,
                "Rambla",
                "Partido social",
                4,
                1,
                false,
                null,
                List.of(new MatchParticipantResponse(10L, 100L, "Player One", MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z"))),
                Instant.parse("2026-03-17T10:00:00Z"),
                Instant.parse("2026-03-17T10:00:00Z")
        );
        when(matchService.createMatch(eq("player@example.com"), any(CreateMatchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/matches")
                        .principal(new TestingAuthenticationToken("player@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduledAt": "2026-03-20T20:00:00Z",
                                  "locationText": "Rambla",
                                  "notes": "Partido social"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/matches/1"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.currentPlayerCount").value(1));
    }

    @Test
    void getMatchesIsPublic() throws Exception {
        when(matchService.getMatches()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].participants[0].fullName").value("Player One"))
                .andExpect(jsonPath("$[0].participants[0].team").value("TEAM_ONE"));
    }

    @Test
    void createInviteLinkRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/matches/1/invite-link"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createInviteLinkReturnsOfficialShareLink() throws Exception {
        when(matchInviteService.createInviteLink("player@example.com", 1L)).thenReturn(
                new MatchInviteLinkResponse(
                        1L,
                        "token-123",
                        "http://localhost:3000?matchInvite=token-123",
                        Instant.parse("2026-04-21T20:00:00Z")
                )
        );

        mockMvc.perform(post("/api/matches/1/invite-link")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(1))
                .andExpect(jsonPath("$.inviteToken").value("token-123"))
                .andExpect(jsonPath("$.inviteUrl").value("http://localhost:3000?matchInvite=token-123"));
    }

    @Test
    void resolveInviteIsPublic() throws Exception {
        when(matchInviteService.resolveInvite("token-123")).thenReturn(
                new MatchInvitePreviewResponse(
                        1L,
                        MatchStatus.OPEN,
                        Instant.parse("2026-04-14T20:00:00Z"),
                        7L,
                        "Top Padel",
                        "Cancha 1",
                        "Top Padel - Cancha 1",
                        "Player One",
                        2,
                        4,
                        Instant.parse("2026-04-21T20:00:00Z")
                )
        );

        mockMvc.perform(get("/api/matches/invite?token=token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(1))
                .andExpect(jsonPath("$.clubName").value("Top Padel"))
                .andExpect(jsonPath("$.courtName").value("Cancha 1"))
                .andExpect(jsonPath("$.currentPlayerCount").value(2));
    }

    @Test
    void assignTeamsReturnsUpdatedParticipants() throws Exception {
        MatchResponse response = buildResponse();
        when(matchService.assignTeams(eq("player@example.com"), eq(1L), any(AssignMatchTeamsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/matches/1/teams")
                        .principal(new TestingAuthenticationToken("player@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignments": [
                                    { "playerProfileId": 10, "team": "TEAM_ONE" },
                                    { "playerProfileId": 11, "team": "TEAM_ONE" },
                                    { "playerProfileId": 12, "team": "TEAM_TWO" },
                                    { "playerProfileId": 13, "team": "TEAM_TWO" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].team").value("TEAM_ONE"));
    }

    @Test
    void cancelMatchReturnsUpdatedStatus() throws Exception {
        MatchResponse response = new MatchResponse(
                1L,
                10L,
                MatchStatus.CANCELLED,
                Instant.parse("2026-03-20T20:00:00Z"),
                null,
                "Rambla",
                "Partido social",
                4,
                1,
                false,
                null,
                List.of(new MatchParticipantResponse(10L, 100L, "Player One", MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z"))),
                Instant.parse("2026-03-17T10:00:00Z"),
                Instant.parse("2026-03-17T10:10:00Z")
        );
        when(matchService.cancelMatch("player@example.com", 1L)).thenReturn(response);

        mockMvc.perform(post("/api/matches/1/cancel")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void submitResultReturnsCreatedResponse() throws Exception {
        MatchResultResponse response = new MatchResultResponse(
                1L,
                10L,
                MatchResultStatus.PENDING,
                MatchWinnerTeam.TEAM_ONE,
                new MatchScoreResponse(6, 3),
                Instant.parse("2026-03-17T10:15:00Z"),
                null,
                null,
                null,
                null,
                null
        );
        when(matchService.submitResult(eq("player@example.com"), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/matches/1/result")
                        .principal(new TestingAuthenticationToken("player@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "winnerTeam": "TEAM_ONE",
                                  "score": {
                                    "teamOneScore": 6,
                                    "teamTwoScore": 3
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/matches/1/result"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.winnerTeam").value("TEAM_ONE"))
                .andExpect(jsonPath("$.score.teamOneScore").value(6));
    }

    @Test
    void confirmResultReturnsConfirmedResult() throws Exception {
        MatchResultResponse response = new MatchResultResponse(
                1L,
                10L,
                MatchResultStatus.CONFIRMED,
                MatchWinnerTeam.TEAM_ONE,
                new MatchScoreResponse(6, 3),
                Instant.parse("2026-03-17T10:15:00Z"),
                12L,
                Instant.parse("2026-03-17T10:20:00Z"),
                null,
                null,
                null
        );
        when(matchService.confirmResult("player@example.com", 1L)).thenReturn(response);

        mockMvc.perform(post("/api/matches/1/result/confirm")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedByPlayerProfileId").value(12));
    }

    @Test
    void rejectResultReturnsRejectedResult() throws Exception {
        MatchResultResponse response = new MatchResultResponse(
                1L,
                10L,
                MatchResultStatus.REJECTED,
                MatchWinnerTeam.TEAM_ONE,
                new MatchScoreResponse(6, 3),
                Instant.parse("2026-03-17T10:15:00Z"),
                null,
                null,
                12L,
                Instant.parse("2026-03-17T10:21:00Z"),
                "Score mal cargado"
        );
        when(matchService.rejectResult(eq("player@example.com"), eq(1L), any(RejectMatchResultRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/matches/1/result/reject")
                        .principal(new TestingAuthenticationToken("player@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "Score mal cargado"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectedByPlayerProfileId").value(12))
                .andExpect(jsonPath("$.rejectionReason").value("Score mal cargado"));
    }

    @Test
    void getMatchResultIsPublic() throws Exception {
        MatchResultResponse response = new MatchResultResponse(
                1L,
                10L,
                MatchResultStatus.PENDING,
                MatchWinnerTeam.TEAM_TWO,
                new MatchScoreResponse(4, 6),
                Instant.parse("2026-03-17T10:15:00Z"),
                null,
                null,
                null,
                null,
                null
        );
        when(matchService.getMatchResult(1L)).thenReturn(response);

        mockMvc.perform(get("/api/matches/1/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.winnerTeam").value("TEAM_TWO"))
                .andExpect(jsonPath("$.score.teamTwoScore").value(6));
    }

    private MatchResponse buildResponse() {
        return new MatchResponse(
                1L,
                10L,
                MatchStatus.OPEN,
                Instant.parse("2026-03-20T20:00:00Z"),
                null,
                "Rambla",
                "Partido social",
                4,
                4,
                true,
                new MatchResultSummaryResponse(
                        MatchResultStatus.PENDING,
                        MatchWinnerTeam.TEAM_ONE,
                        new MatchScoreResponse(6, 3),
                        Instant.parse("2026-03-17T10:15:00Z"),
                        10L,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                List.of(
                        new MatchParticipantResponse(10L, 100L, "Player One", MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:00:00Z")),
                        new MatchParticipantResponse(11L, 101L, "Player Two", MatchParticipantTeam.TEAM_ONE, Instant.parse("2026-03-17T10:01:00Z")),
                        new MatchParticipantResponse(12L, 102L, "Player Three", MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:02:00Z")),
                        new MatchParticipantResponse(13L, 103L, "Player Four", MatchParticipantTeam.TEAM_TWO, Instant.parse("2026-03-17T10:03:00Z"))
                ),
                Instant.parse("2026-03-17T10:00:00Z"),
                Instant.parse("2026-03-17T10:00:00Z")
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
