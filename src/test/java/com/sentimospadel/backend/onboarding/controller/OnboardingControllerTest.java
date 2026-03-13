package com.sentimospadel.backend.onboarding.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimospadel.backend.onboarding.dto.InitialSurveyRequest;
import com.sentimospadel.backend.onboarding.dto.InitialSurveyResponse;
import com.sentimospadel.backend.onboarding.enums.AnswerOption;
import com.sentimospadel.backend.onboarding.service.OnboardingService;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
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
class OnboardingControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        OnboardingController controller = new OnboardingController(onboardingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationRequiredFilter())
                .build();
    }

    @Test
    void getInitialSurveyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/onboarding/initial-survey"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitInitialSurveyReturnsCreatedResponse() throws Exception {
        InitialSurveyRequest request = new InitialSurveyRequest(
                AnswerOption.B,
                AnswerOption.C,
                AnswerOption.D,
                AnswerOption.A,
                AnswerOption.B,
                AnswerOption.C,
                AnswerOption.D,
                AnswerOption.A,
                AnswerOption.B,
                AnswerOption.C
        );

        InitialSurveyResponse response = new InitialSurveyResponse(
                1L,
                1,
                request.q1(),
                request.q2(),
                request.q3(),
                request.q4(),
                request.q5(),
                request.q6(),
                request.q7(),
                request.q8(),
                request.q9(),
                request.q10(),
                68,
                new BigDecimal("17.00"),
                new BigDecimal("3.55"),
                UruguayCategory.QUINTA,
                false,
                ClubVerificationStatus.NOT_REQUIRED,
                Instant.parse("2026-03-13T15:00:00Z"),
                Instant.parse("2026-03-13T15:00:00Z")
        );

        when(onboardingService.submitInitialSurvey(eq("player@example.com"), any(InitialSurveyRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/onboarding/initial-survey")
                        .principal(new TestingAuthenticationToken("player@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/onboarding/initial-survey"))
                .andExpect(jsonPath("$.initialRating").value(3.55))
                .andExpect(jsonPath("$.estimatedCategory").value("QUINTA"))
                .andExpect(jsonPath("$.clubVerificationStatus").value("NOT_REQUIRED"));
    }

    @Test
    void getInitialSurveyReturnsNotFoundWhenNoResultExists() throws Exception {
        when(onboardingService.getInitialSurvey("player@example.com"))
                .thenThrow(new ResourceNotFoundException("Initial survey result for the authenticated user was not found"));

        mockMvc.perform(get("/api/onboarding/initial-survey")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Initial survey result for the authenticated user was not found"));
    }

    private static class AuthenticationRequiredFilter extends OncePerRequestFilter {

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
