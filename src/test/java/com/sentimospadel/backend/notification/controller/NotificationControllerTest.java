package com.sentimospadel.backend.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.notification.dto.NotificationResponse;
import com.sentimospadel.backend.notification.enums.NotificationStatus;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.notification.service.PlayerInboxService;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private PlayerInboxService playerInboxService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(playerInboxService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationRequiredFilter())
                .build();
    }

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotificationsReturnsActiveNotifications() throws Exception {
        when(playerInboxService.getMyNotifications("player@example.com")).thenReturn(List.of(
                new NotificationResponse(
                        10L,
                        PendingActionType.CONFIRM_MATCH_RESULT,
                        NotificationStatus.UNREAD,
                        "Confirmá el resultado del partido",
                        "Tu rival cargó el resultado.",
                        88L,
                        null,
                        null,
                        true,
                        null,
                        Instant.parse("2026-03-25T22:31:00Z"),
                        Instant.parse("2026-03-25T22:31:00Z")
                )
        ));

        mockMvc.perform(get("/api/notifications")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].type").value("CONFIRM_MATCH_RESULT"))
                .andExpect(jsonPath("$[0].status").value("UNREAD"));
    }

    @Test
    void markNotificationAsReadReturnsUpdatedNotification() throws Exception {
        when(playerInboxService.markNotificationAsRead("player@example.com", 10L)).thenReturn(
                new NotificationResponse(
                        10L,
                        PendingActionType.CONFIRM_MATCH_RESULT,
                        NotificationStatus.READ,
                        "Confirmá el resultado del partido",
                        "Tu rival cargó el resultado.",
                        88L,
                        null,
                        null,
                        true,
                        Instant.parse("2026-03-25T22:35:00Z"),
                        Instant.parse("2026-03-25T22:31:00Z"),
                        Instant.parse("2026-03-25T22:35:00Z")
                )
        );

        mockMvc.perform(post("/api/notifications/10/read")
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("READ"));
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
