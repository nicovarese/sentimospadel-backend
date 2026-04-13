package com.sentimospadel.backend.notification.service;

public interface PushGateway {

    String providerName();

    PushGatewayResult send(PushGatewayRequest request);
}
