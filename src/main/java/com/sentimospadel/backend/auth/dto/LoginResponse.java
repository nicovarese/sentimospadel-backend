package com.sentimospadel.backend.auth.dto;

import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long id,
        String email,
        UserRole role,
        UserStatus status,
        Long managedClubId,
        String managedClubName
) {
}
