package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.notification.config.NotificationPushProperties;
import com.sentimospadel.backend.notification.entity.PlayerNotification;
import com.sentimospadel.backend.notification.entity.PushDeviceInstallation;
import com.sentimospadel.backend.notification.entity.PushNotificationDelivery;
import com.sentimospadel.backend.notification.enums.PushDeliveryStatus;
import com.sentimospadel.backend.notification.repository.PushDeviceInstallationRepository;
import com.sentimospadel.backend.notification.repository.PushNotificationDeliveryRepository;
import com.sentimospadel.backend.user.entity.User;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushNotificationDeliveryService {

    private static final String REASON_PUSH_DISABLED = "PUSH_DISABLED";
    private static final String REASON_OPERATIONAL_NOTIFICATIONS_DISABLED = "OPERATIONAL_NOTIFICATIONS_DISABLED";

    private final PushDeviceInstallationRepository pushDeviceInstallationRepository;
    private final PushNotificationDeliveryRepository pushNotificationDeliveryRepository;
    private final PushGateway pushGateway;
    private final NotificationPushProperties notificationPushProperties;

    @Transactional
    public void dispatchNotification(PlayerNotification notification) {
        User user = notification.getPlayerProfile().getUser();
        if (user == null || user.getId() == null || notification.getId() == null) {
            return;
        }

        List<PushDeviceInstallation> activeInstallations =
                pushDeviceInstallationRepository.findAllByUserIdAndActiveTrue(user.getId());

        if (activeInstallations.isEmpty()) {
            return;
        }

        for (PushDeviceInstallation installation : activeInstallations) {
            if (wasAlreadyAttemptedForCurrentNotificationState(notification, installation.getId())) {
                continue;
            }

            PushNotificationDelivery delivery = pushNotificationDeliveryRepository.saveAndFlush(PushNotificationDelivery.builder()
                    .notification(notification)
                    .installation(installation)
                    .status(PushDeliveryStatus.QUEUED)
                    .provider(pushGateway.providerName())
                    .build());

            if (!notificationPushProperties.enabled()) {
                markSkipped(delivery, REASON_PUSH_DISABLED);
                continue;
            }

            if (!user.isOperationalNotificationsEnabled()) {
                markSkipped(delivery, REASON_OPERATIONAL_NOTIFICATIONS_DISABLED);
                continue;
            }

            Instant attemptedAt = Instant.now();
            PushGatewayResult result = pushGateway.send(new PushGatewayRequest(
                    installation.getPushToken(),
                    installation.getPlatform(),
                    notification.getId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getMatchId(),
                    notification.getTournamentId(),
                    notification.getTournamentMatchId()
            ));

            delivery.setAttemptedAt(attemptedAt);
            delivery.setProviderMessageId(result.providerMessageId());
            delivery.setReason(result.reason());

            if (result.success()) {
                delivery.setStatus(PushDeliveryStatus.SENT);
                delivery.setDeliveredAt(attemptedAt);
            } else {
                delivery.setStatus(PushDeliveryStatus.FAILED);
            }

            pushNotificationDeliveryRepository.saveAndFlush(delivery);
        }
    }

    private void markSkipped(PushNotificationDelivery delivery, String reason) {
        Instant now = Instant.now();
        delivery.setStatus(PushDeliveryStatus.SKIPPED);
        delivery.setReason(reason);
        delivery.setAttemptedAt(now);
        pushNotificationDeliveryRepository.saveAndFlush(delivery);
    }

    private boolean wasAlreadyAttemptedForCurrentNotificationState(PlayerNotification notification, Long installationId) {
        return pushNotificationDeliveryRepository.findTopByNotificationIdAndInstallationIdOrderByCreatedAtDesc(
                        notification.getId(),
                        installationId
                )
                .map(delivery -> {
                    if (delivery.getAttemptedAt() == null) {
                        return false;
                    }
                    return !delivery.getAttemptedAt().isBefore(notification.getUpdatedAt());
                })
                .orElse(false);
    }
}
