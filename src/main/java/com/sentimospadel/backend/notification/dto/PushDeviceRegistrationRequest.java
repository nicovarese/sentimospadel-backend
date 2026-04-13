package com.sentimospadel.backend.notification.dto;

import com.sentimospadel.backend.notification.enums.PushDevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushDeviceRegistrationRequest(
        @NotBlank(message = "installationId is required")
        @Size(max = 120, message = "installationId must be at most 120 characters")
        String installationId,
        @NotNull(message = "platform is required")
        PushDevicePlatform platform,
        @NotBlank(message = "pushToken is required")
        @Size(max = 512, message = "pushToken must be at most 512 characters")
        String pushToken
) {
}
