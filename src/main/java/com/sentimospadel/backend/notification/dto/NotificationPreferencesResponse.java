package com.sentimospadel.backend.notification.dto;

import java.time.Instant;

public record NotificationPreferencesResponse(
        boolean activityTrackingEnabled,
        Instant activityTrackingUpdatedAt,
        boolean operationalNotificationsEnabled,
        Instant operationalNotificationsUpdatedAt,
        String consentPreferencesVersion
) {
}
