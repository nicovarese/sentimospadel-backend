package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.notification.dto.NotificationResponse;
import com.sentimospadel.backend.notification.entity.PlayerNotification;
import com.sentimospadel.backend.notification.enums.NotificationStatus;
import com.sentimospadel.backend.notification.repository.PlayerNotificationRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerNotificationService {

    private final PlayerNotificationRepository playerNotificationRepository;
    private final PushNotificationDeliveryService pushNotificationDeliveryService;

    @Transactional
    public Map<String, PlayerNotification> syncPendingActionNotifications(
            PlayerProfile playerProfile,
            List<PendingActionCandidate> candidates
    ) {
        List<PlayerNotification> existingNotifications =
                playerNotificationRepository.findAllByPlayerProfileIdAndManagedBySyncTrueOrderByCreatedAtDesc(playerProfile.getId());

        Map<String, PlayerNotification> notificationsByActionKey = existingNotifications.stream()
                .collect(Collectors.toMap(
                        PlayerNotification::getActionKey,
                        notification -> notification,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<String> desiredActionKeys = candidates.stream()
                .map(PendingActionCandidate::actionKey)
                .collect(Collectors.toSet());

        boolean dirty = false;
        List<PlayerNotification> notificationsToDispatch = new java.util.ArrayList<>();

        for (PlayerNotification notification : existingNotifications) {
            if (notification.isActive() && !desiredActionKeys.contains(notification.getActionKey())) {
                notification.setActive(false);
                dirty = true;
            }
        }

        for (PendingActionCandidate candidate : candidates) {
            PlayerNotification existingNotification = notificationsByActionKey.get(candidate.actionKey());
            if (existingNotification == null) {
                PlayerNotification notification = PlayerNotification.builder()
                        .playerProfile(playerProfile)
                        .type(candidate.type())
                        .status(NotificationStatus.UNREAD)
                        .actionKey(candidate.actionKey())
                        .title(candidate.title())
                        .message(candidate.message())
                        .matchId(candidate.matchId())
                        .tournamentId(candidate.tournamentId())
                        .tournamentMatchId(candidate.tournamentMatchId())
                        .active(true)
                        .managedBySync(true)
                        .build();
                existingNotifications.add(notification);
                notificationsByActionKey.put(candidate.actionKey(), notification);
                dirty = true;
                notificationsToDispatch.add(notification);
                continue;
            }

            if (!existingNotification.isActive()) {
                existingNotification.setActive(true);
                existingNotification.setStatus(NotificationStatus.UNREAD);
                existingNotification.setReadAt(null);
                dirty = true;
                notificationsToDispatch.add(existingNotification);
            }

            if (existingNotification.getType() != candidate.type()) {
                existingNotification.setType(candidate.type());
                dirty = true;
            }
            if (!Objects.equals(existingNotification.getTitle(), candidate.title())) {
                existingNotification.setTitle(candidate.title());
                dirty = true;
            }
            if (!Objects.equals(existingNotification.getMessage(), candidate.message())) {
                existingNotification.setMessage(candidate.message());
                dirty = true;
            }
            if (!Objects.equals(existingNotification.getMatchId(), candidate.matchId())) {
                existingNotification.setMatchId(candidate.matchId());
                dirty = true;
            }
            if (!Objects.equals(existingNotification.getTournamentId(), candidate.tournamentId())) {
                existingNotification.setTournamentId(candidate.tournamentId());
                dirty = true;
            }
            if (!Objects.equals(existingNotification.getTournamentMatchId(), candidate.tournamentMatchId())) {
                existingNotification.setTournamentMatchId(candidate.tournamentMatchId());
                dirty = true;
            }
        }

        if (dirty) {
            playerNotificationRepository.saveAllAndFlush(existingNotifications);
        }

        for (PlayerNotification notification : notificationsToDispatch) {
            pushNotificationDeliveryService.dispatchNotification(notification);
        }

        return notificationsByActionKey;
    }

    @Transactional
    public void publishEventNotification(
            PlayerProfile playerProfile,
            PendingActionType type,
            String actionKey,
            String title,
            String message,
            Long matchId,
            Long tournamentId,
            Long tournamentMatchId
    ) {
        if (playerNotificationRepository.findByPlayerProfileIdAndActionKey(playerProfile.getId(), actionKey).isPresent()) {
            return;
        }

        PlayerNotification savedNotification = playerNotificationRepository.saveAndFlush(PlayerNotification.builder()
                .playerProfile(playerProfile)
                .type(type)
                .status(NotificationStatus.UNREAD)
                .actionKey(actionKey)
                .title(title)
                .message(message)
                .matchId(matchId)
                .tournamentId(tournamentId)
                .tournamentMatchId(tournamentMatchId)
                .active(true)
                .managedBySync(false)
                .build());

        pushNotificationDeliveryService.dispatchNotification(savedNotification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getActiveNotifications(Long playerProfileId) {
        return playerNotificationRepository.findAllByPlayerProfileIdAndActiveTrueOrderByCreatedAtDesc(playerProfileId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long playerProfileId, Long notificationId) {
        PlayerNotification notification = playerNotificationRepository.findByIdAndPlayerProfileId(notificationId, playerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification with id " + notificationId + " was not found"));

        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(Instant.now());
            playerNotificationRepository.saveAndFlush(notification);
        }

        return toResponse(notification);
    }

    public NotificationResponse toResponse(PlayerNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getMatchId(),
                notification.getTournamentId(),
                notification.getTournamentMatchId(),
                notification.isActive(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
