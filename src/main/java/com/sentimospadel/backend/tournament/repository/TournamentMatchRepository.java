package com.sentimospadel.backend.tournament.repository;

import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select distinct tm
            from TournamentMatch tm
            join fetch tm.tournament tournament
            join fetch tm.teamOneEntry teamOneEntry
            left join fetch teamOneEntry.primaryPlayerProfile
            left join fetch teamOneEntry.primaryPlayerProfile.user
            left join fetch teamOneEntry.secondaryPlayerProfile
            left join fetch teamOneEntry.secondaryPlayerProfile.user
            join fetch tm.teamTwoEntry teamTwoEntry
            left join fetch teamTwoEntry.primaryPlayerProfile
            left join fetch teamTwoEntry.primaryPlayerProfile.user
            left join fetch teamTwoEntry.secondaryPlayerProfile
            left join fetch teamTwoEntry.secondaryPlayerProfile.user
            where teamOneEntry.primaryPlayerProfile.id = :playerProfileId
               or teamOneEntry.secondaryPlayerProfile.id = :playerProfileId
               or teamTwoEntry.primaryPlayerProfile.id = :playerProfileId
               or teamTwoEntry.secondaryPlayerProfile.id = :playerProfileId
            order by tm.scheduledAt desc, tm.id desc
            """)
    List<TournamentMatch> findAllByParticipantPlayerProfileIdOrderByScheduledAtDesc(@Param("playerProfileId") Long playerProfileId);

    boolean existsByTournamentId(Long tournamentId);

    long countByTournamentId(Long tournamentId);
}
