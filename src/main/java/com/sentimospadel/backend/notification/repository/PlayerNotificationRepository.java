package com.sentimospadel.backend.notification.repository;

import com.sentimospadel.backend.notification.entity.PlayerNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerNotificationRepository extends JpaRepository<PlayerNotification, Long> {

    List<PlayerNotification> findAllByPlayerProfileIdOrderByCreatedAtDesc(Long playerProfileId);

    List<PlayerNotification> findAllByPlayerProfileIdAndActiveTrueOrderByCreatedAtDesc(Long playerProfileId);

    Optional<PlayerNotification> findByIdAndPlayerProfileId(Long id, Long playerProfileId);
}
