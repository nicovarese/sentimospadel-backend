package com.sentimospadel.backend.verification.dto;

import com.sentimospadel.backend.verification.enums.ClubVerificationRequestStatus;
import java.time.Instant;

public record PlayerClubVerificationRequestResponse(
        Long id,
        Long clubId,
        String clubName,
        String clubCity,
        ClubVerificationRequestStatus status,
        Instant requestedAt,
        Instant reviewedAt,
        String reviewNotes
) {
}
