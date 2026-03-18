package com.sentimospadel.backend.match.repository;

import com.sentimospadel.backend.match.entity.MatchParticipant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    @EntityGraph(attributePaths = {"playerProfile", "playerProfile.user"})
    List<MatchParticipant> findAllByMatchIdOrderByJoinedAtAsc(Long matchId);

    @EntityGraph(attributePaths = {"match", "match.createdBy", "match.createdBy.user", "match.club", "playerProfile", "playerProfile.user"})
    List<MatchParticipant> findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(Long playerProfileId);

    @EntityGraph(attributePaths = {"match", "playerProfile", "playerProfile.user"})
    List<MatchParticipant> findAllByMatchIdInOrderByJoinedAtAsc(Collection<Long> matchIds);

    boolean existsByMatchIdAndPlayerProfileId(Long matchId, Long playerProfileId);

    long countByMatchId(Long matchId);

    Optional<MatchParticipant> findByMatchIdAndPlayerProfileId(Long matchId, Long playerProfileId);
}
