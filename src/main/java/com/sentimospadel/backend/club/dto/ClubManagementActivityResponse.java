package com.sentimospadel.backend.club.dto;

import java.time.Instant;

public record ClubManagementActivityResponse(
        Long id,
        String title,
        String description,
        Instant occurredAt
) {
}
