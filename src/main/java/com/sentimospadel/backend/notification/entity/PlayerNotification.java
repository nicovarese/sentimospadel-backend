package com.sentimospadel.backend.notification.entity;

import com.sentimospadel.backend.notification.enums.NotificationStatus;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "player_notifications")
public class PlayerNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_profile_id", nullable = false)
    private PlayerProfile playerProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 60)
    private PendingActionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "action_key", nullable = false, length = 160)
    private String actionKey;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "tournament_id")
    private Long tournamentId;

    @Column(name = "tournament_match_id")
    private Long tournamentMatchId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "read_at")
    private Instant readAt;
}
