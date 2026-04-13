package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.legal.service.LegalDocumentService;
import com.sentimospadel.backend.notification.dto.NotificationPreferencesResponse;
import com.sentimospadel.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final UserRepository userRepository;
    private final LegalDocumentService legalDocumentService;

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences(String email) {
        return toResponse(requireUser(email));
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(String email, UpdateNotificationPreferencesRequest request) {
        User user = requireUser(email);
        legalDocumentService.validateConsentPreferencesVersion(request.consentPreferencesVersion());

        Instant now = Instant.now();
        user.setActivityTrackingEnabled(Boolean.TRUE.equals(request.allowActivityTracking()));
        user.setActivityTrackingUpdatedAt(now);
        user.setOperationalNotificationsEnabled(Boolean.TRUE.equals(request.allowOperationalNotifications()));
        user.setOperationalNotificationsUpdatedAt(now);
        user.setConsentPreferencesVersion(request.consentPreferencesVersion().trim());

        return toResponse(userRepository.saveAndFlush(user));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " was not found"));
    }

    private NotificationPreferencesResponse toResponse(User user) {
        return new NotificationPreferencesResponse(
                user.isActivityTrackingEnabled(),
                user.getActivityTrackingUpdatedAt(),
                user.isOperationalNotificationsEnabled(),
                user.getOperationalNotificationsUpdatedAt(),
                user.getConsentPreferencesVersion()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
