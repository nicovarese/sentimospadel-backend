package com.sentimospadel.backend.player.dto;

import com.sentimospadel.backend.player.enums.PreferredSide;
import java.time.Instant;

public record PlayerProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String photoUrl,
        PreferredSide preferredSide,
        String declaredLevel,
        String city,
        String bio,
        Integer currentElo,
        boolean provisional,
        Integer matchesPlayed,
        Instant createdAt,
        Instant updatedAt
) {
}
