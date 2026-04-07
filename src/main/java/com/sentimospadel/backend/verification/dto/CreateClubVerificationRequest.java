package com.sentimospadel.backend.verification.dto;

import jakarta.validation.constraints.NotNull;

public record CreateClubVerificationRequest(
        @NotNull Long clubId
) {
}
