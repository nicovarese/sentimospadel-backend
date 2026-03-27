package com.sentimospadel.backend.notification.controller;

import com.sentimospadel.backend.notification.dto.NotificationResponse;
import com.sentimospadel.backend.notification.service.PlayerInboxService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PlayerInboxService playerInboxService;

    @GetMapping
    public List<NotificationResponse> getMyNotifications(Authentication authentication) {
        return playerInboxService.getMyNotifications(authentication.getName());
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markNotificationAsRead(Authentication authentication, @PathVariable Long id) {
        return playerInboxService.markNotificationAsRead(authentication.getName(), id);
    }
}
