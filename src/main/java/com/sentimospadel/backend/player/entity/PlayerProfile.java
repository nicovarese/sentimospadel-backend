package com.sentimospadel.backend.player.entity;

import com.sentimospadel.backend.player.enums.PreferredSide;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
@Table(name = "player_profiles")
public class PlayerProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_side", length = 20)
    private PreferredSide preferredSide;

    @Column(name = "declared_level", length = 50)
    private String declaredLevel;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "current_elo", nullable = false)
    private Integer currentElo;

    @Column(name = "provisional", nullable = false)
    private boolean provisional;

    @Column(name = "matches_played", nullable = false)
    private Integer matchesPlayed;
}
