package com.sentimospadel.backend.tournament.entity;

import com.sentimospadel.backend.shared.persistence.BaseEntity;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
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
@Table(name = "tournament_matches")
public class TournamentMatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_one_entry_id", nullable = false)
    private TournamentEntry teamOneEntry;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_two_entry_id", nullable = false)
    private TournamentEntry teamTwoEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 30)
    private TournamentMatchPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TournamentMatchStatus status;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "leg_number")
    private Integer legNumber;

    @Column(name = "round_label", nullable = false, length = 160)
    private String roundLabel;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "court_name", length = 160)
    private String courtName;
}
