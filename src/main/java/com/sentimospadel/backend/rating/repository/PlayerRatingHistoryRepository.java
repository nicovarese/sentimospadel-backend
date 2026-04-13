package com.sentimospadel.backend.rating.repository;

import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRatingHistoryRepository extends JpaRepository<PlayerRatingHistory, Long> {

    long countByMatchId(Long matchId);

    long countByTournamentMatchId(Long tournamentMatchId);

    @EntityGraph(attributePaths = {
            "match",
            "tournamentMatch",
            "tournamentMatch.tournament",
            "tournamentMatch.teamOneEntry",
            "tournamentMatch.teamOneEntry.primaryPlayerProfile",
            "tournamentMatch.teamOneEntry.secondaryPlayerProfile",
            "tournamentMatch.teamTwoEntry",
            "tournamentMatch.teamTwoEntry.primaryPlayerProfile",
            "tournamentMatch.teamTwoEntry.secondaryPlayerProfile"
    })
    List<PlayerRatingHistory> findAllByPlayerProfileIdOrderByCreatedAtDesc(Long playerProfileId);

    @EntityGraph(attributePaths = {"match"})
    List<PlayerRatingHistory> findAllByPlayerProfileIdAndMatchIsNotNullOrderByCreatedAtDesc(Long playerProfileId);
}
