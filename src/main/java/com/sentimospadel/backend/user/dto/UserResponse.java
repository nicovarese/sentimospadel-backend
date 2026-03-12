package com.sentimospadel.backend.user.dto;

import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
