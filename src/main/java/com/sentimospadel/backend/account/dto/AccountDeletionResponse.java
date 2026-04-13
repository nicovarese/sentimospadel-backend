package com.sentimospadel.backend.account.dto;

import java.time.Instant;

public record AccountDeletionResponse(
        boolean requested,
        Instant requestedAt,
        String message
) {
}
