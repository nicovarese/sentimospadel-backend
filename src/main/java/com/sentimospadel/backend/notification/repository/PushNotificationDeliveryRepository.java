package com.sentimospadel.backend.notification.repository;

import com.sentimospadel.backend.notification.entity.PushNotificationDelivery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushNotificationDeliveryRepository extends JpaRepository<PushNotificationDelivery, Long> {

    Optional<PushNotificationDelivery> findTopByNotificationIdAndInstallationIdOrderByCreatedAtDesc(
            Long notificationId,
            Long installationId
    );
}
