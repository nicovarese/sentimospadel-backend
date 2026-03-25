package com.sentimospadel.backend.tournament.entity;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "tournament_match_results")
public class TournamentMatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "tournament_match_id", nullable = false, unique = true)
    private TournamentMatch tournamentMatch;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submitted_by_player_id", nullable = false)
    private PlayerProfile submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TournamentMatchResultStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_team", nullable = false, length = 20)
    private MatchWinnerTeam winnerTeam;

    @Column(name = "set_one_team_one_games")
    private Integer setOneTeamOneGames;

    @Column(name = "set_one_team_two_games")
    private Integer setOneTeamTwoGames;

    @Column(name = "set_two_team_one_games")
    private Integer setTwoTeamOneGames;

    @Column(name = "set_two_team_two_games")
    private Integer setTwoTeamTwoGames;

    @Column(name = "set_three_team_one_games")
    private Integer setThreeTeamOneGames;

    @Column(name = "set_three_team_two_games")
    private Integer setThreeTeamTwoGames;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @ManyToOne
    @JoinColumn(name = "confirmed_by_player_id")
    private PlayerProfile confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @ManyToOne
    @JoinColumn(name = "rejected_by_player_id")
    private PlayerProfile rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
