package com.sentimospadel.backend.verification.dto;

import jakarta.validation.constraints.Size;

public record ClubVerificationDecisionRequest(
        @Size(max = 1000) String notes
) {
}
