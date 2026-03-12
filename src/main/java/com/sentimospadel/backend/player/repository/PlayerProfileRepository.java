package com.sentimospadel.backend.player.repository;

import com.sentimospadel.backend.player.entity.PlayerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<PlayerProfile> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "user")
    java.util.List<PlayerProfile> findAll();

    Optional<PlayerProfile> findByUserId(Long userId);
}
