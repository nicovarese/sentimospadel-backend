package com.sentimospadel.backend.club.dto;

import java.time.Instant;

public record ClubResponse(
        Long id,
        String name,
        String city,
        String address,
        String description,
        boolean integrated,
        Instant createdAt,
        Instant updatedAt
) {
}
