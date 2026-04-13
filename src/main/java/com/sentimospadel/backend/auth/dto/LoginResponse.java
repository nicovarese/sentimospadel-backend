package com.sentimospadel.backend.auth.dto;

import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String tokenType,
        Long id,
        String email,
        UserRole role,
        UserStatus status,
        Long managedClubId,
        String managedClubName
) {
}
