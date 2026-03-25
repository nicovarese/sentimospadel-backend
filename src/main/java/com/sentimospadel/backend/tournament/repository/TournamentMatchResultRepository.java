package com.sentimospadel.backend.tournament.repository;

import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentMatchResultRepository extends JpaRepository<TournamentMatchResult, Long> {

    @EntityGraph(attributePaths = {
            "tournamentMatch",
            "submittedBy",
            "confirmedBy",
            "rejectedBy"
    })
    Optional<TournamentMatchResult> findByTournamentMatchId(Long tournamentMatchId);

    @EntityGraph(attributePaths = {
            "tournamentMatch",
            "submittedBy",
            "confirmedBy",
            "rejectedBy"
    })
    List<TournamentMatchResult> findAllByTournamentMatchTournamentId(Long tournamentId);
}
