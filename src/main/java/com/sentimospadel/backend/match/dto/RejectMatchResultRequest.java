package com.sentimospadel.backend.match.dto;

import jakarta.validation.constraints.Size;

public record RejectMatchResultRequest(
        @Size(max = 255, message = "rejectionReason must be at most 255 characters")
        String rejectionReason
) {
}
