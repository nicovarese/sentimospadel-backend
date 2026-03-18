package com.sentimospadel.backend.rating.entity;

import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "player_rating_history")
public class PlayerRatingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_profile_id", nullable = false)
    private PlayerProfile playerProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "old_rating", nullable = false, precision = 4, scale = 2)
    private BigDecimal oldRating;

    @Column(name = "delta", nullable = false, precision = 5, scale = 2)
    private BigDecimal delta;

    @Column(name = "new_rating", nullable = false, precision = 4, scale = 2)
    private BigDecimal newRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
