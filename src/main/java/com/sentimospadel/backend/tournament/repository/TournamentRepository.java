package com.sentimospadel.backend.tournament.repository;

import com.sentimospadel.backend.tournament.entity.Tournament;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Override
    @EntityGraph(attributePaths = {"createdBy", "createdBy.user", "club"})
    Optional<Tournament> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"createdBy", "createdBy.user", "club"})
    List<Tournament> findAll();
}
