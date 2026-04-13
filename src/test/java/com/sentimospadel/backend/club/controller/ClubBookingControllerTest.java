package com.sentimospadel.backend.club.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.club.dto.ClubBookingAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubBookingCourtResponse;
import com.sentimospadel.backend.club.dto.ClubBookingSlotResponse;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.enums.ClubBookingMode;
import com.sentimospadel.backend.club.service.ClubBookingService;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ClubBookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClubBookingService clubBookingService;

    @BeforeEach
    void setUp() {
        ClubBookingController controller = new ClubBookingController(clubBookingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getBookingAvailabilityReturnsPublicClubAgenda() throws Exception {
        when(clubBookingService.getBookingAgenda(7L, LocalDate.parse("2026-04-07"))).thenReturn(
                new ClubBookingAgendaResponse(
                        7L,
                        "Top Padel",
                        ClubBookingMode.DIRECT,
                        LocalDate.parse("2026-04-07"),
                        List.of(
                                new ClubBookingCourtResponse(
                                        3L,
                                        "Cancha 3",
                                        new BigDecimal("900.00"),
                                        List.of(
                                                new ClubBookingSlotResponse("19:00", ClubAgendaSlotStatus.AVAILABLE),
                                                new ClubBookingSlotResponse("20:30", ClubAgendaSlotStatus.RESERVED)
                                        )
                                )
                        )
                )
        );

        mockMvc.perform(get("/api/clubs/7/booking-availability")
                        .queryParam("date", "2026-04-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubId").value(7))
                .andExpect(jsonPath("$.courts[0].name").value("Cancha 3"))
                .andExpect(jsonPath("$.courts[0].hourlyRateUyu").value(900.00))
                .andExpect(jsonPath("$.courts[0].slots[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.courts[0].slots[1].status").value("RESERVED"));
    }
}
