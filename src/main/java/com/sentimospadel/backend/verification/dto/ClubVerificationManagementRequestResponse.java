package com.sentimospadel.backend.verification.dto;

import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.verification.enums.ClubVerificationRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ClubVerificationManagementRequestResponse(
        Long id,
        Long playerProfileId,
        String playerFullName,
        String playerPhotoUrl,
        String playerCity,
        BigDecimal currentRating,
        UruguayCategory currentCategory,
        Instant requestedAt,
        ClubVerificationRequestStatus status,
        Instant reviewedAt,
        String reviewNotes
) {
}
