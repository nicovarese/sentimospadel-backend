package com.sentimospadel.backend.verification.dto;

import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import java.util.List;

public record PlayerClubVerificationSummaryResponse(
        boolean requiresClubVerification,
        ClubVerificationStatus clubVerificationStatus,
        boolean canCreateRequest,
        List<PlayerClubVerificationRequestResponse> requests
) {
}
