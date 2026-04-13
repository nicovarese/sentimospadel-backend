package com.sentimospadel.backend.notification.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNotificationPreferencesRequest(
        @NotNull(message = "allowActivityTracking is required")
        Boolean allowActivityTracking,
        @NotNull(message = "allowOperationalNotifications is required")
        Boolean allowOperationalNotifications,
        @Size(max = 40, message = "Consent preferences version must be at most 40 characters")
        String consentPreferencesVersion
) {
}
