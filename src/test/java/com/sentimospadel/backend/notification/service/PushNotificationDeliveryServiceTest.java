package com.sentimospadel.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.notification.config.NotificationPushProperties;
import com.sentimospadel.backend.notification.entity.PlayerNotification;
import com.sentimospadel.backend.notification.entity.PushDeviceInstallation;
import com.sentimospadel.backend.notification.entity.PushNotificationDelivery;
import com.sentimospadel.backend.notification.enums.NotificationStatus;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.notification.enums.PushDeliveryStatus;
import com.sentimospadel.backend.notification.enums.PushDevicePlatform;
import com.sentimospadel.backend.notification.repository.PushDeviceInstallationRepository;
import com.sentimospadel.backend.notification.repository.PushNotificationDeliveryRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushNotificationDeliveryServiceTest {

    @Mock
    private PushDeviceInstallationRepository pushDeviceInstallationRepository;

    @Mock
    private PushNotificationDeliveryRepository pushNotificationDeliveryRepository;

    @Mock
    private PushGateway pushGateway;

    private PushNotificationDeliveryService pushNotificationDeliveryService;

    @BeforeEach
    void setUp() {
        pushNotificationDeliveryService = new PushNotificationDeliveryService(
                pushDeviceInstallationRepository,
                pushNotificationDeliveryRepository,
                pushGateway,
                new NotificationPushProperties(true, true, "log-only", "", "", "https://oauth2.googleapis.com/token", "https://fcm.googleapis.com/v1/projects/%s/messages:send")
        );
    }

    @Test
    void dispatchNotificationMarksDeliverySkippedWhenOperationalNotificationsAreDisabled() {
        PlayerNotification notification = notification(false);
        PushDeviceInstallation installation = installation(notification.getPlayerProfile().getUser(), 44L, "ios-install");

        when(pushDeviceInstallationRepository.findAllByUserIdAndActiveTrue(10L)).thenReturn(List.of(installation));
        when(pushNotificationDeliveryRepository.findTopByNotificationIdAndInstallationIdOrderByCreatedAtDesc(20L, 44L))
                .thenReturn(Optional.empty());
        when(pushGateway.providerName()).thenReturn("log-only");
        AtomicReference<PushNotificationDelivery> deliveryRef = new AtomicReference<>();
        when(pushNotificationDeliveryRepository.saveAndFlush(any(PushNotificationDelivery.class)))
                .thenAnswer(invocation -> {
                    PushNotificationDelivery delivery = invocation.getArgument(0);
                    deliveryRef.set(delivery);
                    return delivery;
                });

        pushNotificationDeliveryService.dispatchNotification(notification);

        verify(pushNotificationDeliveryRepository, times(2)).saveAndFlush(any(PushNotificationDelivery.class));
        assertThat(deliveryRef.get()).isNotNull();
        assertThat(deliveryRef.get().getStatus()).isEqualTo(PushDeliveryStatus.SKIPPED);
        assertThat(deliveryRef.get().getReason()).isEqualTo("OPERATIONAL_NOTIFICATIONS_DISABLED");
        verify(pushGateway, never()).send(any(PushGatewayRequest.class));
    }

    @Test
    void dispatchNotificationSendsToGatewayWhenUserIsEligibleAndDeviceExists() {
        PlayerNotification notification = notification(true);
        PushDeviceInstallation installation = installation(notification.getPlayerProfile().getUser(), 44L, "android-install");

        when(pushDeviceInstallationRepository.findAllByUserIdAndActiveTrue(10L)).thenReturn(List.of(installation));
        when(pushNotificationDeliveryRepository.findTopByNotificationIdAndInstallationIdOrderByCreatedAtDesc(20L, 44L))
                .thenReturn(Optional.empty());
        when(pushGateway.providerName()).thenReturn("log-only");
        when(pushGateway.send(any(PushGatewayRequest.class))).thenReturn(new PushGatewayResult(true, "provider-123", null));
        AtomicReference<PushNotificationDelivery> deliveryRef = new AtomicReference<>();
        when(pushNotificationDeliveryRepository.saveAndFlush(any(PushNotificationDelivery.class)))
                .thenAnswer(invocation -> {
                    PushNotificationDelivery delivery = invocation.getArgument(0);
                    deliveryRef.set(delivery);
                    return delivery;
                });

        pushNotificationDeliveryService.dispatchNotification(notification);

        verify(pushGateway).send(any(PushGatewayRequest.class));
        verify(pushNotificationDeliveryRepository, times(2)).saveAndFlush(any(PushNotificationDelivery.class));
        assertThat(deliveryRef.get()).isNotNull();
        assertThat(deliveryRef.get().getStatus()).isEqualTo(PushDeliveryStatus.SENT);
        assertThat(deliveryRef.get().getProviderMessageId()).isEqualTo("provider-123");
        assertThat(deliveryRef.get().getDeliveredAt()).isNotNull();
    }

    @Test
    void dispatchNotificationSkipsWhenCurrentNotificationStateWasAlreadyAttempted() {
        PlayerNotification notification = notification(true);
        PushDeviceInstallation installation = installation(notification.getPlayerProfile().getUser(), 44L, "android-install");
        PushNotificationDelivery previousDelivery = PushNotificationDelivery.builder()
                .notification(notification)
                .installation(installation)
                .status(PushDeliveryStatus.SENT)
                .provider("log-only")
                .attemptedAt(Instant.parse("2026-04-07T21:31:00Z"))
                .deliveredAt(Instant.parse("2026-04-07T21:31:00Z"))
                .build();

        when(pushDeviceInstallationRepository.findAllByUserIdAndActiveTrue(10L)).thenReturn(List.of(installation));
        when(pushNotificationDeliveryRepository.findTopByNotificationIdAndInstallationIdOrderByCreatedAtDesc(20L, 44L))
                .thenReturn(Optional.of(previousDelivery));

        pushNotificationDeliveryService.dispatchNotification(notification);

        verify(pushNotificationDeliveryRepository, never()).saveAndFlush(any(PushNotificationDelivery.class));
        verify(pushGateway, never()).send(any(PushGatewayRequest.class));
    }

    private PlayerNotification notification(boolean operationalNotificationsEnabled) {
        User user = User.builder()
                .id(10L)
                .email("player@example.com")
                .phone("091234567")
                .passwordHash("hash")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .operationalNotificationsEnabled(operationalNotificationsEnabled)
                .build();

        PlayerProfile playerProfile = PlayerProfile.builder()
                .id(10L)
                .user(user)
                .fullName("Player")
                .currentRating(BigDecimal.valueOf(4.50))
                .provisional(false)
                .matchesPlayed(10)
                .ratedMatchesCount(10)
                .surveyCompleted(true)
                .requiresClubVerification(false)
                .clubVerificationStatus(com.sentimospadel.backend.player.enums.ClubVerificationStatus.NOT_REQUIRED)
                .build();

        PlayerNotification notification = PlayerNotification.builder()
                .id(20L)
                .playerProfile(playerProfile)
                .type(PendingActionType.TOURNAMENT_LAUNCHED)
                .status(NotificationStatus.UNREAD)
                .actionKey("event-tournament-launched-20")
                .title("Tu torneo ya fue lanzado")
                .message("Ya tiene partidos agendados.")
                .tournamentId(20L)
                .active(true)
                .managedBySync(false)
                .build();
        notification.setUpdatedAt(Instant.parse("2026-04-07T21:30:00Z"));
        return notification;
    }

    private PushDeviceInstallation installation(User user, Long id, String installationId) {
        return PushDeviceInstallation.builder()
                .id(id)
                .user(user)
                .installationId(installationId)
                .platform(PushDevicePlatform.ANDROID)
                .pushToken("token-123")
                .active(true)
                .build();
    }
}
