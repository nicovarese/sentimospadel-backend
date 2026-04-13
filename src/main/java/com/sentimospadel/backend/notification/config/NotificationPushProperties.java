package com.sentimospadel.backend.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.push")
public record NotificationPushProperties(
        boolean enabled,
        boolean logPayloads,
        String provider,
        String fcmProjectId,
        String fcmServiceAccountJsonBase64,
        String fcmAccessTokenUri,
        String fcmEndpoint
) {
}
