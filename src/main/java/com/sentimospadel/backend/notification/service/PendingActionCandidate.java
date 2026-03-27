package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.notification.enums.PendingActionType;
import java.time.Instant;

record PendingActionCandidate(
        String actionKey,
        PendingActionType type,
        Long matchId,
        Long tournamentId,
        Long tournamentMatchId,
        String title,
        String message,
        Instant scheduledAt,
        Instant dueAt
) {
}
