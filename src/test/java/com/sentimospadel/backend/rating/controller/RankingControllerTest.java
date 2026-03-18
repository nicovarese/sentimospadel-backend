package com.sentimospadel.backend.rating.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.rating.dto.RankingEntryResponse;
import com.sentimospadel.backend.rating.service.RankingService;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RankingControllerTest {

    @Mock
    private RankingService rankingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RankingController(rankingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getRankingsReturnsOrderedPlayers() throws Exception {
        when(rankingService.getRankings()).thenReturn(List.of(
                new RankingEntryResponse(1, 10L, "Player One", "Montevideo", new BigDecimal("5.62"), UruguayCategory.SEGUNDA, 12),
                new RankingEntryResponse(2, 11L, "Player Two", "Canelones", new BigDecimal("5.10"), UruguayCategory.TERCERA, 8)
        ));

        mockMvc.perform(get("/api/rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].playerProfileId").value(10))
                .andExpect(jsonPath("$[0].currentRating").value(5.62))
                .andExpect(jsonPath("$[0].currentCategory").value("SEGUNDA"))
                .andExpect(jsonPath("$[1].position").value(2));
    }
}
