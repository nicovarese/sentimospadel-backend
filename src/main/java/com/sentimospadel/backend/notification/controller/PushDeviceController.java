package com.sentimospadel.backend.notification.controller;

import com.sentimospadel.backend.notification.dto.PushDeviceRegistrationRequest;
import com.sentimospadel.backend.notification.dto.PushDeviceResponse;
import com.sentimospadel.backend.notification.dto.PushDeviceUnregisterRequest;
import com.sentimospadel.backend.notification.service.PushDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/devices")
@RequiredArgsConstructor
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    @PostMapping("/register")
    public PushDeviceResponse registerDevice(
            Authentication authentication,
            @Valid @RequestBody PushDeviceRegistrationRequest request
    ) {
        return pushDeviceService.registerDevice(authentication.getName(), request);
    }

    @PostMapping("/unregister")
    public PushDeviceResponse unregisterDevice(
            Authentication authentication,
            @Valid @RequestBody PushDeviceUnregisterRequest request
    ) {
        return pushDeviceService.unregisterDevice(authentication.getName(), request);
    }
}
