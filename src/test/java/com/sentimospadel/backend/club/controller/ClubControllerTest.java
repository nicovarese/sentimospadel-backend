package com.sentimospadel.backend.club.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimospadel.backend.club.dto.ClubResponse;
import com.sentimospadel.backend.club.dto.CreateClubRequest;
import com.sentimospadel.backend.club.service.ClubService;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ClubService clubService;

    @BeforeEach
    void setUp() {
        ClubController controller = new ClubController(clubService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createClubReturnsCreatedResponse() throws Exception {
        ClubResponse response = new ClubResponse(
                1L,
                "Montevideo Padel Club",
                "Montevideo",
                "Av. Italia 1234",
                "Indoor padel courts",
                true,
                Instant.parse("2026-03-12T12:00:00Z"),
                Instant.parse("2026-03-12T12:00:00Z")
        );

        when(clubService.createClub(any(CreateClubRequest.class))).thenReturn(response);

        CreateClubRequest request = new CreateClubRequest(
                "Montevideo Padel Club",
                "Montevideo",
                "Av. Italia 1234",
                "Indoor padel courts",
                true
        );

        mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/clubs/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Montevideo Padel Club"));
    }

    @Test
    void createClubReturnsBadRequestForInvalidPayload() throws Exception {
        CreateClubRequest request = new CreateClubRequest(
                "",
                "",
                null,
                null,
                false
        );

        mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").exists());
    }
}
