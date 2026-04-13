package com.sentimospadel.backend.club.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.club.dto.ClubManagementAgendaCourtResponse;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaSlotResponse;
import com.sentimospadel.backend.club.dto.ClubManagementCourtResponse;
import com.sentimospadel.backend.club.dto.ClubManagementCourtsResponse;
import com.sentimospadel.backend.club.dto.ClubManagementDashboardResponse;
import com.sentimospadel.backend.club.dto.ClubManagementUsersResponse;
import com.sentimospadel.backend.club.dto.ClubQuickActionResponse;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.service.ClubManagementService;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import com.sentimospadel.backend.verification.dto.ClubVerificationManagementRequestResponse;
import com.sentimospadel.backend.verification.enums.ClubVerificationRequestStatus;
import com.sentimospadel.backend.verification.service.ClubVerificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ClubManagementControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClubManagementService clubManagementService;

    @Mock
    private ClubVerificationService clubVerificationService;

    @BeforeEach
    void setUp() {
        ClubManagementController controller = new ClubManagementController(clubManagementService, clubVerificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDashboardReturnsManagedClubData() throws Exception {
        when(clubManagementService.getDashboard("club.admin@sentimospadel.test"))
                .thenReturn(new ClubManagementDashboardResponse(
                        1L,
                        "Top Padel",
                        2,
                        3,
                        BigDecimal.valueOf(3600),
                        3,
                        List.of()
                ));

        mockMvc.perform(get("/api/clubs/me/management/dashboard").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("Top Padel"))
                .andExpect(jsonPath("$.activeCourtsCount").value(2))
                .andExpect(jsonPath("$.todayReservationsCount").value(3));
    }

    @Test
    void getUsersReturnsOverview() throws Exception {
        when(clubManagementService.getUsersOverview("club.admin@sentimospadel.test"))
                .thenReturn(new ClubManagementUsersResponse(
                        1L,
                        "Top Padel",
                        24,
                        5,
                        3,
                        BigDecimal.valueOf(2450),
                        BigDecimal.valueOf(4.2),
                        BigDecimal.valueOf(3.8),
                        BigDecimal.valueOf(3.5),
                        List.of()
                ));

        mockMvc.perform(get("/api/clubs/me/management/users").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("Top Padel"))
                .andExpect(jsonPath("$.activeUsersCount").value(24))
                .andExpect(jsonPath("$.newUsersThisMonthCount").value(5));
    }

    @Test
    void getCourtsReturnsManagedCourtConfiguration() throws Exception {
        when(clubManagementService.getCourts("club.admin@sentimospadel.test"))
                .thenReturn(new ClubManagementCourtsResponse(
                        1L,
                        "Top Padel",
                        2,
                        3,
                        List.of(
                                new ClubManagementCourtResponse(10L, "Cancha 1", 1, BigDecimal.valueOf(1200), true),
                                new ClubManagementCourtResponse(11L, "Cancha 2", 2, BigDecimal.valueOf(1200), false)
                        )
                ));

        mockMvc.perform(get("/api/clubs/me/management/courts").principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCourtsCount").value(2))
                .andExpect(jsonPath("$.totalCourtsCount").value(3))
                .andExpect(jsonPath("$.courts[1].active").value(false));
    }

    @Test
    void createCourtReturnsUpdatedSnapshot() throws Exception {
        when(clubManagementService.createCourt(eq("club.admin@sentimospadel.test"), any()))
                .thenReturn(new ClubManagementCourtsResponse(
                        1L,
                        "Top Padel",
                        3,
                        3,
                        List.of(new ClubManagementCourtResponse(12L, "Cancha 3", 3, BigDecimal.valueOf(1150), true))
                ));

        mockMvc.perform(post("/api/clubs/me/management/courts")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cancha 3",
                                  "hourlyRateUyu": 1150
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courts[0].name").value("Cancha 3"))
                .andExpect(jsonPath("$.courts[0].hourlyRateUyu").value(1150));
    }

    @Test
    void updateCourtReturnsUpdatedSnapshot() throws Exception {
        when(clubManagementService.updateCourt(eq("club.admin@sentimospadel.test"), eq(10L), any()))
                .thenReturn(new ClubManagementCourtsResponse(
                        1L,
                        "Top Padel",
                        1,
                        2,
                        List.of(new ClubManagementCourtResponse(10L, "Cancha Central", 1, BigDecimal.valueOf(1400), true))
                ));

        mockMvc.perform(put("/api/clubs/me/management/courts/10")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Cancha Central",
                                  "hourlyRateUyu": 1400,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courts[0].name").value("Cancha Central"));
    }

    @Test
    void reorderCourtsReturnsUpdatedSnapshot() throws Exception {
        when(clubManagementService.reorderCourts(eq("club.admin@sentimospadel.test"), any()))
                .thenReturn(new ClubManagementCourtsResponse(
                        1L,
                        "Top Padel",
                        2,
                        2,
                        List.of(
                                new ClubManagementCourtResponse(11L, "Cancha 2", 1, BigDecimal.valueOf(1200), true),
                                new ClubManagementCourtResponse(10L, "Cancha 1", 2, BigDecimal.valueOf(1200), true)
                        )
                ));

        mockMvc.perform(post("/api/clubs/me/management/courts/reorder")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderedCourtIds": [11, 10]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courts[0].id").value(11))
                .andExpect(jsonPath("$.courts[0].displayOrder").value(1));
    }

    @Test
    void getAgendaReturnsCourtSlots() throws Exception {
        when(clubManagementService.getAgenda(eq("club.admin@sentimospadel.test"), eq(LocalDate.of(2026, 3, 26))))
                .thenReturn(new ClubManagementAgendaResponse(
                        1L,
                        "Top Padel",
                        LocalDate.of(2026, 3, 26),
                        List.of(new ClubManagementAgendaCourtResponse(
                                10L,
                                "Cancha 1 (Cristal)",
                                List.of(new ClubManagementAgendaSlotResponse(
                                        "10|2026-03-26|19:00",
                                        "19:00",
                                        ClubAgendaSlotStatus.RESERVED,
                                        "Reserva Premium",
                                        null
                                ))
                        ))
                ));

        mockMvc.perform(get("/api/clubs/me/management/agenda")
                        .param("date", "2026-03-26")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courts[0].name").value("Cancha 1 (Cristal)"))
                .andExpect(jsonPath("$.courts[0].slots[0].status").value("RESERVED"));
    }

    @Test
    void applyAgendaSlotActionReturnsUpdatedAgenda() throws Exception {
        when(clubManagementService.applyAgendaSlotAction(eq("club.admin@sentimospadel.test"), any()))
                .thenReturn(new ClubManagementAgendaResponse(
                        1L,
                        "Top Padel",
                        LocalDate.of(2026, 3, 26),
                        List.of()
                ));

        mockMvc.perform(post("/api/clubs/me/management/agenda/slot-action")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-03-26",
                                  "courtId": 10,
                                  "time": "19:00:00",
                                  "action": "BLOCK"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("Top Padel"));
    }

    @Test
    void approvePendingBookingReturnsUpdatedAgenda() throws Exception {
        when(clubManagementService.approvePendingBooking("club.admin@sentimospadel.test", 30L))
                .thenReturn(new ClubManagementAgendaResponse(1L, "World Padel", LocalDate.of(2026, 3, 26), List.of()));

        mockMvc.perform(post("/api/clubs/me/management/booking-requests/30/approve")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("World Padel"));
    }

    @Test
    void rejectPendingBookingReturnsUpdatedAgenda() throws Exception {
        when(clubManagementService.rejectPendingBooking("club.admin@sentimospadel.test", 30L))
                .thenReturn(new ClubManagementAgendaResponse(1L, "World Padel", LocalDate.of(2026, 3, 26), List.of()));

        mockMvc.perform(post("/api/clubs/me/management/booking-requests/30/reject")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("World Padel"));
    }

    @Test
    void executeQuickActionReturnsMessage() throws Exception {
        when(clubManagementService.executeQuickAction(eq("club.admin@sentimospadel.test"), any()))
                .thenReturn(new ClubQuickActionResponse("Registro operativo guardado: aviso a usuarios"));

        mockMvc.perform(post("/api/clubs/me/management/quick-actions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "NOTIFY_USERS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro operativo guardado: aviso a usuarios"));
    }

    @Test
    void getVerificationRequestsReturnsManagedClubQueue() throws Exception {
        when(clubVerificationService.getManagedClubRequests("club.admin@sentimospadel.test"))
                .thenReturn(List.of(
                        new ClubVerificationManagementRequestResponse(
                                10L,
                                100L,
                                "Player One",
                                null,
                                "Montevideo",
                                BigDecimal.valueOf(5.90),
                                UruguayCategory.SEGUNDA,
                                Instant.parse("2026-03-31T13:00:00Z"),
                                ClubVerificationRequestStatus.PENDING,
                                null,
                                null
                        )
                ));

        mockMvc.perform(get("/api/clubs/me/management/verification-requests")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerFullName").value("Player One"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void approveVerificationRequestReturnsReviewedRequest() throws Exception {
        when(clubVerificationService.approveManagedClubRequest(eq("club.admin@sentimospadel.test"), eq(10L), any()))
                .thenReturn(new ClubVerificationManagementRequestResponse(
                        10L,
                        100L,
                        "Player One",
                        null,
                        "Montevideo",
                        BigDecimal.valueOf(5.90),
                        UruguayCategory.SEGUNDA,
                        Instant.parse("2026-03-31T13:00:00Z"),
                        ClubVerificationRequestStatus.APPROVED,
                        Instant.parse("2026-03-31T15:00:00Z"),
                        "Validado en torneo interno"
                ));

        mockMvc.perform(post("/api/clubs/me/management/verification-requests/10/approve")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notes": "Validado en torneo interno"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewNotes").value("Validado en torneo interno"));
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken("club.admin@sentimospadel.test", "n/a", List.of());
    }
}


