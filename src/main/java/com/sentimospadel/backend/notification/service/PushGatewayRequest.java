package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.notification.enums.PushDevicePlatform;

public record PushGatewayRequest(
        String token,
        PushDevicePlatform platform,
        Long notificationId,
        PendingActionType type,
        String title,
        String message,
        Long matchId,
        Long tournamentId,
        Long tournamentMatchId
) {
}
