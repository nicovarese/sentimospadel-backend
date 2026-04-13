package com.sentimospadel.backend.notification.dto;

import com.sentimospadel.backend.notification.enums.PushDevicePlatform;
import java.time.Instant;

public record PushDeviceResponse(
        String installationId,
        PushDevicePlatform platform,
        boolean active,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
) {
}
