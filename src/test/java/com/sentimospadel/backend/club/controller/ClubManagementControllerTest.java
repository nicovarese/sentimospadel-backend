package com.sentimospadel.backend.club.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.club.dto.ClubManagementAgendaCourtResponse;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaSlotResponse;
import com.sentimospadel.backend.club.dto.ClubManagementDashboardResponse;
import com.sentimospadel.backend.club.dto.ClubManagementUsersResponse;
import com.sentimospadel.backend.club.dto.ClubQuickActionResponse;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.service.ClubManagementService;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
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

    @BeforeEach
    void setUp() {
        ClubManagementController controller = new ClubManagementController(clubManagementService);
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
                                        "Reserva Premium"
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
    void executeQuickActionReturnsMessage() throws Exception {
        when(clubManagementService.executeQuickAction(eq("club.admin@sentimospadel.test"), any()))
                .thenReturn(new ClubQuickActionResponse("Notificación enviada a usuarios activos"));

        mockMvc.perform(post("/api/clubs/me/management/quick-actions")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "NOTIFY_USERS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notificación enviada a usuarios activos"));
    }

    private Authentication authentication() {
        return new UsernamePasswordAuthenticationToken("club.admin@sentimospadel.test", "n/a", List.of());
    }
}
