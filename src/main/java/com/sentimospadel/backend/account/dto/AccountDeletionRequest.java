package com.sentimospadel.backend.account.dto;

import jakarta.validation.constraints.Size;

public record AccountDeletionRequest(
        @Size(max = 1000)
        String reason
) {
}
