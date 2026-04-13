package com.sentimospadel.backend.club.dto;

import com.sentimospadel.backend.club.enums.ClubBookingMode;
import java.time.Instant;

public record ClubResponse(
        Long id,
        String name,
        String city,
        String address,
        String description,
        boolean integrated,
        ClubBookingMode bookingMode,
        Instant createdAt,
        Instant updatedAt
) {
}
