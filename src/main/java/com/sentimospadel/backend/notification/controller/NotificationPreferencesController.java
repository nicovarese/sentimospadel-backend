package com.sentimospadel.backend.notification.controller;

import com.sentimospadel.backend.notification.dto.NotificationPreferencesResponse;
import com.sentimospadel.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.sentimospadel.backend.notification.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping
    public NotificationPreferencesResponse getPreferences(Authentication authentication) {
        return notificationPreferenceService.getPreferences(authentication.getName());
    }

    @PutMapping
    public NotificationPreferencesResponse updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request
    ) {
        return notificationPreferenceService.updatePreferences(authentication.getName(), request);
    }
}
