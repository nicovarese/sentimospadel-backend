package com.sentimospadel.backend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushDeviceUnregisterRequest(
        @NotBlank(message = "installationId is required")
        @Size(max = 120, message = "installationId must be at most 120 characters")
        String installationId
) {
}
