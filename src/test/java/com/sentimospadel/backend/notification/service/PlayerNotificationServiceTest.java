package com.sentimospadel.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.notification.entity.PlayerNotification;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.notification.repository.PlayerNotificationRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerNotificationServiceTest {

    @Mock
    private PlayerNotificationRepository playerNotificationRepository;

    @Mock
    private PushNotificationDeliveryService pushNotificationDeliveryService;

    private PlayerNotificationService playerNotificationService;

    @BeforeEach
    void setUp() {
        playerNotificationService = new PlayerNotificationService(playerNotificationRepository, pushNotificationDeliveryService);
    }

    @Test
    void syncPendingActionNotificationsCreatesManagedNotifications() {
        PlayerProfile playerProfile = playerProfile(10L, "player@example.com");
        PendingActionCandidate candidate = new PendingActionCandidate(
                "submit-match-88",
                PendingActionType.SUBMIT_MATCH_RESULT,
                88L,
                null,
                null,
                "Carga el resultado del partido",
                "Tu partido ya termino.",
                Instant.parse("2026-04-07T20:00:00Z"),
                Instant.parse("2026-04-07T21:30:00Z")
        );

        when(playerNotificationRepository.findAllByPlayerProfileIdAndManagedBySyncTrueOrderByCreatedAtDesc(10L))
                .thenReturn(new ArrayList<>());

        Map<String, PlayerNotification> notifications = playerNotificationService.syncPendingActionNotifications(playerProfile, List.of(candidate));

        ArgumentCaptor<List<PlayerNotification>> notificationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerNotificationRepository).saveAllAndFlush(notificationsCaptor.capture());

        PlayerNotification savedNotification = notificationsCaptor.getValue().getFirst();
        assertThat(savedNotification.isManagedBySync()).isTrue();
        assertThat(savedNotification.isActive()).isTrue();
        assertThat(savedNotification.getType()).isEqualTo(PendingActionType.SUBMIT_MATCH_RESULT);
        assertThat(notifications).containsKey("submit-match-88");
        verify(pushNotificationDeliveryService, times(1)).dispatchNotification(savedNotification);
    }

    @Test
    void publishEventNotificationCreatesUnreadNotificationOutsideSyncLifecycle() {
        PlayerProfile playerProfile = playerProfile(10L, "player@example.com");
        when(playerNotificationRepository.findByPlayerProfileIdAndActionKey(10L, "event-tournament-launched-5"))
                .thenReturn(Optional.empty());
        when(playerNotificationRepository.saveAndFlush(any(PlayerNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        playerNotificationService.publishEventNotification(
                playerProfile,
                PendingActionType.TOURNAMENT_LAUNCHED,
                "event-tournament-launched-5",
                "Tu torneo ya fue lanzado",
                "El torneo ya fue lanzado.",
                null,
                5L,
                null
        );

        ArgumentCaptor<PlayerNotification> notificationCaptor = ArgumentCaptor.forClass(PlayerNotification.class);
        verify(playerNotificationRepository).saveAndFlush(notificationCaptor.capture());

        PlayerNotification savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.isManagedBySync()).isFalse();
        assertThat(savedNotification.isActive()).isTrue();
        assertThat(savedNotification.getTournamentId()).isEqualTo(5L);
        assertThat(savedNotification.getType()).isEqualTo(PendingActionType.TOURNAMENT_LAUNCHED);
        verify(pushNotificationDeliveryService).dispatchNotification(savedNotification);
    }

    @Test
    void publishEventNotificationIsIdempotentPerActionKey() {
        PlayerProfile playerProfile = playerProfile(10L, "player@example.com");
        when(playerNotificationRepository.findByPlayerProfileIdAndActionKey(10L, "event-match-cancelled-88"))
                .thenReturn(Optional.of(PlayerNotification.builder()
                        .playerProfile(playerProfile)
                        .type(PendingActionType.MATCH_CANCELLED)
                        .status(com.sentimospadel.backend.notification.enums.NotificationStatus.UNREAD)
                        .actionKey("event-match-cancelled-88")
                        .title("Se cancelo tu partido")
                        .message("Ya existe")
                        .active(true)
                        .managedBySync(false)
                        .build()));

        playerNotificationService.publishEventNotification(
                playerProfile,
                PendingActionType.MATCH_CANCELLED,
                "event-match-cancelled-88",
                "Se cancelo tu partido",
                "El partido fue cancelado.",
                88L,
                null,
                null
        );

        verify(playerNotificationRepository, never()).saveAndFlush(any(PlayerNotification.class));
        verify(pushNotificationDeliveryService, never()).dispatchNotification(any(PlayerNotification.class));
    }

    @Test
    void syncPendingActionNotificationsRedispatchesWhenInactiveNotificationBecomesActiveAgain() {
        PlayerProfile playerProfile = playerProfile(10L, "player@example.com");
        PlayerNotification existingNotification = PlayerNotification.builder()
                .playerProfile(playerProfile)
                .type(PendingActionType.SUBMIT_MATCH_RESULT)
                .status(com.sentimospadel.backend.notification.enums.NotificationStatus.READ)
                .actionKey("submit-match-88")
                .title("Carga el resultado del partido")
                .message("Tu partido ya termino.")
                .matchId(88L)
                .active(false)
                .managedBySync(true)
                .build();
        existingNotification.setUpdatedAt(Instant.parse("2026-04-07T21:35:00Z"));

        PendingActionCandidate candidate = new PendingActionCandidate(
                "submit-match-88",
                PendingActionType.SUBMIT_MATCH_RESULT,
                88L,
                null,
                null,
                "Carga el resultado del partido",
                "Tu partido ya termino.",
                Instant.parse("2026-04-07T20:00:00Z"),
                Instant.parse("2026-04-07T21:30:00Z")
        );

        when(playerNotificationRepository.findAllByPlayerProfileIdAndManagedBySyncTrueOrderByCreatedAtDesc(10L))
                .thenReturn(new ArrayList<>(List.of(existingNotification)));

        playerNotificationService.syncPendingActionNotifications(playerProfile, List.of(candidate));

        verify(pushNotificationDeliveryService).dispatchNotification(argThat(notification ->
                notification == existingNotification && notification.isActive()));
    }

    private PlayerProfile playerProfile(Long id, String email) {
        return PlayerProfile.builder()
                .id(id)
                .user(User.builder()
                        .id(id)
                        .email(email)
                        .passwordHash("hash")
                        .role(UserRole.PLAYER)
                        .status(UserStatus.ACTIVE)
                        .build())
                .fullName("Player")
                .currentRating(BigDecimal.valueOf(4.50))
                .provisional(false)
                .matchesPlayed(10)
                .ratedMatchesCount(10)
                .surveyCompleted(true)
                .requiresClubVerification(false)
                .clubVerificationStatus(com.sentimospadel.backend.player.enums.ClubVerificationStatus.NOT_REQUIRED)
                .build();
    }
}
