package com.sentimospadel.backend.notification.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sentimospadel.backend.notification.dto.PushDeviceResponse;
import com.sentimospadel.backend.notification.enums.PushDevicePlatform;
import com.sentimospadel.backend.notification.service.PushDeviceService;
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
class PushDeviceControllerTest {

    @Mock
    private PushDeviceService pushDeviceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PushDeviceController(pushDeviceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new AuthenticationRequiredFilter())
                .build();
    }

    @Test
    void registerDeviceReturnsPersistedInstallation() throws Exception {
        when(pushDeviceService.registerDevice(
                org.mockito.ArgumentMatchers.eq("player@example.com"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new PushDeviceResponse(
                "android-install-1",
                PushDevicePlatform.ANDROID,
                true,
                Instant.parse("2026-04-07T12:00:00Z"),
                Instant.parse("2026-04-07T12:00:00Z"),
                Instant.parse("2026-04-07T12:00:00Z")
        ));

        mockMvc.perform(post("/api/notifications/devices/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "installationId": "android-install-1",
                                  "platform": "ANDROID",
                                  "pushToken": "token-123"
                                }
                                """)
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationId").value("android-install-1"))
                .andExpect(jsonPath("$.platform").value("ANDROID"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void unregisterDeviceReturnsInactiveInstallation() throws Exception {
        when(pushDeviceService.unregisterDevice(
                org.mockito.ArgumentMatchers.eq("player@example.com"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new PushDeviceResponse(
                "android-install-1",
                PushDevicePlatform.ANDROID,
                false,
                Instant.parse("2026-04-07T12:10:00Z"),
                Instant.parse("2026-04-07T12:00:00Z"),
                Instant.parse("2026-04-07T12:10:00Z")
        ));

        mockMvc.perform(post("/api/notifications/devices/unregister")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "installationId": "android-install-1"
                                }
                                """)
                        .principal(new TestingAuthenticationToken("player@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installationId").value("android-install-1"))
                .andExpect(jsonPath("$.active").value(false));
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
