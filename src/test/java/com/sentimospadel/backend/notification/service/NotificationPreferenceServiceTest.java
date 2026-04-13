package com.sentimospadel.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.legal.service.LegalDocumentService;
import com.sentimospadel.backend.notification.dto.NotificationPreferencesResponse;
import com.sentimospadel.backend.notification.dto.UpdateNotificationPreferencesRequest;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LegalDocumentService legalDocumentService;

    private NotificationPreferenceService notificationPreferenceService;

    @BeforeEach
    void setUp() {
        notificationPreferenceService = new NotificationPreferenceService(userRepository, legalDocumentService);
    }

    @Test
    void getPreferencesReturnsPersistedConsentState() {
        User user = user(true, false, "2026-04-07.1");
        user.setActivityTrackingUpdatedAt(Instant.parse("2026-04-07T10:00:00Z"));
        user.setOperationalNotificationsUpdatedAt(Instant.parse("2026-04-07T10:05:00Z"));

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

        NotificationPreferencesResponse response = notificationPreferenceService.getPreferences("player@example.com");

        assertThat(response.activityTrackingEnabled()).isTrue();
        assertThat(response.operationalNotificationsEnabled()).isFalse();
        assertThat(response.consentPreferencesVersion()).isEqualTo("2026-04-07.1");
    }

    @Test
    void updatePreferencesPersistsNewConsentStateAndVersion() {
        User user = user(false, false, "2026-04-07.1");

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        NotificationPreferencesResponse response = notificationPreferenceService.updatePreferences(
                "player@example.com",
                new UpdateNotificationPreferencesRequest(true, true, "2026-04-07.2")
        );

        verify(legalDocumentService).validateConsentPreferencesVersion("2026-04-07.2");
        assertThat(response.activityTrackingEnabled()).isTrue();
        assertThat(response.operationalNotificationsEnabled()).isTrue();
        assertThat(response.consentPreferencesVersion()).isEqualTo("2026-04-07.2");
        assertThat(user.getActivityTrackingUpdatedAt()).isNotNull();
        assertThat(user.getOperationalNotificationsUpdatedAt()).isNotNull();
    }

    private User user(boolean activityTrackingEnabled, boolean operationalNotificationsEnabled, String consentVersion) {
        return User.builder()
                .id(10L)
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hash")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .activityTrackingEnabled(activityTrackingEnabled)
                .operationalNotificationsEnabled(operationalNotificationsEnabled)
                .consentPreferencesVersion(consentVersion)
                .build();
    }
}
