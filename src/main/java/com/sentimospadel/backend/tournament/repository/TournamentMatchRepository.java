package com.sentimospadel.backend.tournament.repository;

import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

    @EntityGraph(attributePaths = {
            "tournament",
            "teamOneEntry",
            "teamOneEntry.primaryPlayerProfile",
            "teamOneEntry.primaryPlayerProfile.user",
            "teamOneEntry.secondaryPlayerProfile",
            "teamOneEntry.secondaryPlayerProfile.user",
            "teamTwoEntry",
            "teamTwoEntry.primaryPlayerProfile",
            "teamTwoEntry.primaryPlayerProfile.user",
            "teamTwoEntry.secondaryPlayerProfile",
            "teamTwoEntry.secondaryPlayerProfile.user"
    })
    List<TournamentMatch> findAllByTournamentIdOrderByRoundNumberAscIdAsc(Long tournamentId);

    @EntityGraph(attributePaths = {
            "tournament",
            "teamOneEntry",
            "teamOneEntry.primaryPlayerProfile",
            "teamOneEntry.primaryPlayerProfile.user",
            "teamOneEntry.secondaryPlayerProfile",
            "teamOneEntry.secondaryPlayerProfile.user",
            "teamTwoEntry",
            "teamTwoEntry.primaryPlayerProfile",
            "teamTwoEntry.primaryPlayerProfile.user",
            "teamTwoEntry.secondaryPlayerProfile",
            "teamTwoEntry.secondaryPlayerProfile.user"
    })
    Optional<TournamentMatch> findByIdAndTournamentId(Long matchId, Long tournamentId);

    boolean existsByTournamentId(Long tournamentId);

    long countByTournamentId(Long tournamentId);
}
