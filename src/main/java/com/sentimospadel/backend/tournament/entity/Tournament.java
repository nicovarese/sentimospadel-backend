package com.sentimospadel.backend.tournament.entity;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.shared.persistence.BaseEntity;
import com.sentimospadel.backend.tournament.enums.TournamentAmericanoType;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
@Table(name = "tournaments")
public class Tournament extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_player_id", nullable = false)
    private PlayerProfile createdBy;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 20)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "americano_type", length = 20)
    private TournamentAmericanoType americanoType;

    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TournamentStatus status;

    @Column(name = "max_entries")
    private Integer maxEntries;

    @Column(name = "open_enrollment", nullable = false)
    private boolean openEnrollment;

    @Column(name = "competitive", nullable = false)
    private boolean competitive;

    @Column(name = "launched_at")
    private Instant launchedAt;

    @Column(name = "available_courts")
    private Integer availableCourts;

    @Column(name = "number_of_groups")
    private Integer numberOfGroups;

    @Column(name = "league_rounds")
    private Integer leagueRounds;

    @Column(name = "points_for_win", nullable = false)
    private Integer pointsForWin;

    @Column(name = "points_for_tiebreak_loss", nullable = false)
    private Integer pointsForTiebreakLoss;

    @Column(name = "points_for_loss", nullable = false)
    private Integer pointsForLoss;

    @Enumerated(EnumType.STRING)
    @Column(name = "standings_tiebreak", nullable = false, length = 30)
    private TournamentStandingsTiebreak standingsTiebreak;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "court_names_json", length = 4000)
    private List<String> courtNames;
}
