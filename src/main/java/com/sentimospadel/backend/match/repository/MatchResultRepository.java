package com.sentimospadel.backend.match.repository;

import com.sentimospadel.backend.match.entity.MatchResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    @EntityGraph(attributePaths = {"match", "submittedBy", "submittedBy.user"})
    Optional<MatchResult> findByMatchId(Long matchId);

    @EntityGraph(attributePaths = {"match"})
    List<MatchResult> findAllByMatchIdIn(Collection<Long> matchIds);

    boolean existsByMatchId(Long matchId);
}
