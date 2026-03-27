package com.sentimospadel.backend.notification.dto;

import com.sentimospadel.backend.notification.enums.NotificationStatus;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import java.time.Instant;

public record PendingActionResponse(
        Long notificationId,
        PendingActionType type,
        NotificationStatus notificationStatus,
        Long matchId,
        Long tournamentId,
        Long tournamentMatchId,
        String title,
        String message,
        Instant scheduledAt,
        Instant dueAt
) {
}
