package com.sentimospadel.backend.player.dto;

import com.sentimospadel.backend.player.enums.PreferredSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdatePlayerProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must be at most 255 characters")
        String fullName,
        @Size(max = 500, message = "Photo URL must be at most 500 characters")
        String photoUrl,
        @NotNull(message = "Preferred side is required")
        PreferredSide preferredSide,
        @NotBlank(message = "Declared level is required")
        @Size(max = 50, message = "Declared level must be at most 50 characters")
        String declaredLevel,
        @NotBlank(message = "City is required")
        @Size(max = 120, message = "City must be at most 120 characters")
        String city,
        @Positive(message = "Represented club id must be positive")
        Long representedClubId,
        @Size(max = 1000, message = "Bio must be at most 1000 characters")
        String bio
) {
}
