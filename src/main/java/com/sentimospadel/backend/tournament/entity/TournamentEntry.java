package com.sentimospadel.backend.tournament.entity;

import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.support.StringListJsonConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
@Table(name = "tournament_entries")
public class TournamentEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_profile_id", nullable = false)
    private PlayerProfile primaryPlayerProfile;

    @ManyToOne
    @JoinColumn(name = "secondary_player_profile_id")
    private PlayerProfile secondaryPlayerProfile;

    @ManyToOne
    @JoinColumn(name = "created_by_player_id")
    private PlayerProfile createdBy;

    @Column(name = "team_name", length = 160)
    private String teamName;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_status", nullable = false, length = 20)
    private TournamentEntryStatus status;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "time_preferences_json", length = 4000)
    private java.util.List<String> timePreferences;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
