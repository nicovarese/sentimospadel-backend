package com.sentimospadel.backend.player.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.media.player-profile-photos")
public record PlayerProfilePhotoStorageProperties(
        @NotBlank String storagePath,
        @NotBlank String publicBaseUrl,
        @Min(1) long maxSizeBytes
) {
}
