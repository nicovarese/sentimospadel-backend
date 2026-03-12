package com.sentimospadel.backend.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClubRequest(
        @NotBlank(message = "Club name is required")
        @Size(max = 255, message = "Club name must be at most 255 characters")
        String name,
        @NotBlank(message = "Club city is required")
        @Size(max = 120, message = "Club city must be at most 120 characters")
        String city,
        @Size(max = 255, message = "Club address must be at most 255 characters")
        String address,
        @Size(max = 1000, message = "Club description must be at most 1000 characters")
        String description,
        boolean integrated
) {
}
