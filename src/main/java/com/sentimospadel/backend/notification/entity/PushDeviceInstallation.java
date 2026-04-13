package com.sentimospadel.backend.notification.entity;

import com.sentimospadel.backend.notification.enums.PushDevicePlatform;
import com.sentimospadel.backend.shared.persistence.BaseEntity;
import com.sentimospadel.backend.user.entity.User;
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
@Table(name = "push_device_installations")
public class PushDeviceInstallation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "installation_id", nullable = false, unique = true, length = 120)
    private String installationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private PushDevicePlatform platform;

    @Column(name = "push_token", nullable = false, length = 512)
    private String pushToken;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
