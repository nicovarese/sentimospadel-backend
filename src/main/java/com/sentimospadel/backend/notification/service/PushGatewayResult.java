package com.sentimospadel.backend.notification.service;

public record PushGatewayResult(
        boolean success,
        String providerMessageId,
        String reason
) {
}
