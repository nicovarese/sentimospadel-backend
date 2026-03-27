package com.sentimospadel.backend.match.repository;

import com.sentimospadel.backend.match.entity.Match;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy", "createdBy.user", "club"})
    Optional<Match> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"createdBy", "createdBy.user", "club"})
    List<Match> findAll();

    @EntityGraph(attributePaths = {"createdBy", "createdBy.user", "club"})
    List<Match> findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(Long clubId, Instant from, Instant to);
}
