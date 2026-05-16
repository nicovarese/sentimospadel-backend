package com.sentimospadel.backend.user.entity;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.shared.persistence.BaseEntity;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone", nullable = true, unique = true, length = 40)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "email_verification_token_hash", length = 64)
    private String emailVerificationTokenHash;

    @Column(name = "email_verification_token_expires_at")
    private Instant emailVerificationTokenExpiresAt;

    @Column(name = "accepted_terms_version", length = 40)
    private String acceptedTermsVersion;

    @Column(name = "accepted_terms_at")
    private Instant acceptedTermsAt;

    @Column(name = "accepted_privacy_version", length = 40)
    private String acceptedPrivacyVersion;

    @Column(name = "accepted_privacy_at")
    private Instant acceptedPrivacyAt;

    @Column(name = "consent_preferences_version", length = 40)
    private String consentPreferencesVersion;

    @Column(name = "activity_tracking_enabled", nullable = false)
    private boolean activityTrackingEnabled;

    @Column(name = "activity_tracking_updated_at")
    private Instant activityTrackingUpdatedAt;

    @Column(name = "operational_notifications_enabled", nullable = false)
    private boolean operationalNotificationsEnabled;

    @Column(name = "operational_notifications_updated_at")
    private Instant operationalNotificationsUpdatedAt;

    @Column(name = "account_deletion_requested_at")
    private Instant accountDeletionRequestedAt;

    @Column(name = "account_deletion_reason", length = 1000)
    private String accountDeletionReason;

    @ManyToOne
    @JoinColumn(name = "managed_club_id")
    private Club managedClub;
}
