package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.notification.dto.NotificationResponse;
import com.sentimospadel.backend.notification.dto.PendingActionResponse;
import com.sentimospadel.backend.notification.entity.PlayerNotification;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerInboxService {

    private final PlayerProfileResolverService playerProfileResolverService;
    private final PendingActionService pendingActionService;
    private final PlayerNotificationService playerNotificationService;

    @Transactional
    public List<PendingActionResponse> getMyPendingActions(String email) {
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);
        List<PendingActionCandidate> candidates = pendingActionService.computePendingActions(playerProfile.getId());
        Map<String, PlayerNotification> notificationsByActionKey =
                playerNotificationService.syncPendingActionNotifications(playerProfile, candidates);

        return candidates.stream()
                .map(candidate -> {
                    PlayerNotification notification = notificationsByActionKey.get(candidate.actionKey());
                    return new PendingActionResponse(
                            notification == null ? null : notification.getId(),
                            candidate.type(),
                            notification == null ? null : notification.getStatus(),
                            candidate.matchId(),
                            candidate.tournamentId(),
                            candidate.tournamentMatchId(),
                            candidate.title(),
                            candidate.message(),
                            candidate.scheduledAt(),
                            candidate.dueAt()
                    );
                })
                .toList();
    }

    @Transactional
    public List<NotificationResponse> getMyNotifications(String email) {
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);
        List<PendingActionCandidate> candidates = pendingActionService.computePendingActions(playerProfile.getId());
        playerNotificationService.syncPendingActionNotifications(playerProfile, candidates);
        return playerNotificationService.getActiveNotifications(playerProfile.getId());
    }

    @Transactional
    public NotificationResponse markNotificationAsRead(String email, Long notificationId) {
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);
        return playerNotificationService.markAsRead(playerProfile.getId(), notificationId);
    }
}
