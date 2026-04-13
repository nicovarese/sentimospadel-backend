package com.sentimospadel.backend.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.notification.dto.NotificationPreferencesResponse;
import com.sentimospadel.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.sentimospadel.backend.notification.service.NotificationPreferenceService;
import com.sentimospadel.backend.shared.exception.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
class NotificationPreferencesControllerTest {

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationPreferencesController(notificationPreferenceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationRequiredFilter())
                .build();
    }

    @Test
    void getPreferencesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPreferencesReturnsStoredConsentState() throws Exception {
        when(notificationPreferenceService.getPreferences("player@example.com")).thenReturn(new NotificationPreferencesResponse(
                true,
                Instant.parse("2026-04-07T10:00:00Z"),
                false,
                Instant.parse("2026-04-07T10:05:00Z"),
                "2026-04-07.1"
        ));

        mockMvc.perform(get("/api/notifications/preferences")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityTrackingEnabled").value(true))
                .andExpect(jsonPath("$.operationalNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.consentPreferencesVersion").value("2026-04-07.1"));
    }

    @Test
    void updatePreferencesReturnsPersistedConsentState() throws Exception {
        when(notificationPreferenceService.updatePreferences(
                org.mockito.ArgumentMatchers.eq("player@example.com"),
                org.mockito.ArgumentMatchers.any(UpdateNotificationPreferencesRequest.class)
        )).thenReturn(new NotificationPreferencesResponse(
                true,
                Instant.parse("2026-04-07T10:10:00Z"),
                true,
                Instant.parse("2026-04-07T10:10:00Z"),
                "2026-04-07.2"
        ));

        mockMvc.perform(put("/api/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "allowActivityTracking": true,
                                  "allowOperationalNotifications": true,
                                  "consentPreferencesVersion": "2026-04-07.2"
                                }
                                """)
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalNotificationsEnabled").value(true))
                .andExpect(jsonPath("$.consentPreferencesVersion").value("2026-04-07.2"));
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
