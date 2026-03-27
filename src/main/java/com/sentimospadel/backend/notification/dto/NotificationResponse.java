package com.sentimospadel.backend.notification.dto;

import com.sentimospadel.backend.notification.enums.NotificationStatus;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        PendingActionType type,
        NotificationStatus status,
        String title,
        String message,
        Long matchId,
        Long tournamentId,
        Long tournamentMatchId,
        boolean active,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt
) {
}
