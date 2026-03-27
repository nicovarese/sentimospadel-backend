package com.sentimospadel.backend.coach.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.coach.dto.CoachResponse;
import com.sentimospadel.backend.coach.service.CoachService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CoachControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CoachService coachService;

    @BeforeEach
    void setUp() {
        CoachController controller = new CoachController(coachService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listsCoaches() throws Exception {
        when(coachService.getCoaches()).thenReturn(List.of(
                new CoachResponse(
                        1L,
                        "Tomi",
                        "Club Reducto",
                        new BigDecimal("5.30"),
                        com.sentimospadel.backend.player.enums.UruguayCategory.TERCERA,
                        70,
                        new BigDecimal("4.80"),
                        1000,
                        "098388097",
                        "https://picsum.photos/100/100?r=tomi",
                        Instant.parse("2026-03-26T10:00:00Z"),
                        Instant.parse("2026-03-26T10:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/coaches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Tomi"))
                .andExpect(jsonPath("$[0].clubName").value("Club Reducto"))
                .andExpect(jsonPath("$[0].currentCategory").value("TERCERA"))
                .andExpect(jsonPath("$[0].hourlyRateUyu").value(1000));
    }
}
