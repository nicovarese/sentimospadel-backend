package com.sentimospadel.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sentimospadel.backend.notification.config.NotificationPushProperties;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.notification.enums.PushDevicePlatform;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LogOnlyPushGatewayTest {

    @Test
    void validateConfigurationDoesNotRequireFcmCredentialsWhenProviderIsLogOnly() {
        LogOnlyPushGateway gateway = new LogOnlyPushGateway(properties(true, "log-only", "", ""));

        gateway.validateConfiguration();

        assertThat(gateway.providerName()).isEqualTo("log-only");
    }

    @Test
    void validateConfigurationRequiresServiceAccountWhenProviderIsFcm() {
        LogOnlyPushGateway gateway = new LogOnlyPushGateway(properties(true, "fcm", "sentimospadel", ""));

        assertThatThrownBy(gateway::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid FCM push notification configuration")
                .hasMessageContaining("FCM service account is not configured");
    }

    @Test
    void validateConfigurationRequiresServiceAccountFieldsWhenProviderIsFcm() {
        String encodedJson = encodeJson("""
                {
                  "project_id": "sentimospadel",
                  "client_email": "",
                  "private_key": "fake-key",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """);
        LogOnlyPushGateway gateway = new LogOnlyPushGateway(properties(true, "fcm", "", encodedJson));

        assertThatThrownBy(gateway::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client_email is required");
    }

    @Test
    void buildFcmMessageIncludesNotificationAndRoutingData() {
        LogOnlyPushGateway gateway = new LogOnlyPushGateway(properties(true, "fcm", "sentimospadel", "unused"));

        Map<String, Object> body = gateway.buildFcmMessage(new PushGatewayRequest(
                "token-123",
                PushDevicePlatform.ANDROID,
                10L,
                PendingActionType.TOURNAMENT_LAUNCHED,
                "Torneo lanzado",
                "Ya tenes partidos agendados.",
                null,
                20L,
                30L
        ));

        Map<?, ?> message = (Map<?, ?>) body.get("message");
        Map<?, ?> notification = (Map<?, ?>) message.get("notification");
        Map<?, ?> data = (Map<?, ?>) message.get("data");

        assertThat(message.get("token")).isEqualTo("token-123");
        assertThat(notification.get("title")).isEqualTo("Torneo lanzado");
        assertThat(notification.get("body")).isEqualTo("Ya tenes partidos agendados.");
        assertThat(data.get("notificationId")).isEqualTo("10");
        assertThat(data.get("type")).isEqualTo("TOURNAMENT_LAUNCHED");
        assertThat(data.get("tournamentId")).isEqualTo("20");
        assertThat(data.get("tournamentMatchId")).isEqualTo("30");
        assertThat(data.containsKey("matchId")).isFalse();
    }

    private NotificationPushProperties properties(
            boolean enabled,
            String provider,
            String projectId,
            String serviceAccountJsonBase64
    ) {
        return new NotificationPushProperties(
                enabled,
                false,
                provider,
                projectId,
                serviceAccountJsonBase64,
                "https://oauth2.googleapis.com/token",
                "https://fcm.googleapis.com/v1/projects/%s/messages:send"
        );
    }

    private String encodeJson(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
