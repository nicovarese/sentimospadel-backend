package com.sentimospadel.backend.notification.entity;

import com.sentimospadel.backend.notification.enums.PushDeliveryStatus;
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
@Table(name = "push_notification_deliveries")
public class PushNotificationDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private PlayerNotification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installation_id", nullable = false)
    private PushDeviceInstallation installation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PushDeliveryStatus status;

    @Column(name = "provider", nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "attempted_at")
    private Instant attemptedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
