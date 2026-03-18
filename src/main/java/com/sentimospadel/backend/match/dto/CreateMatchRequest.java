package com.sentimospadel.backend.match.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateMatchRequest(
        @NotNull Instant scheduledAt,
        Long clubId,
        String locationText,
        String notes
) {
}
