package com.sentimospadel.backend.rating.repository;

import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRatingHistoryRepository extends JpaRepository<PlayerRatingHistory, Long> {

    long countByMatchId(Long matchId);

    @EntityGraph(attributePaths = {"match"})
    List<PlayerRatingHistory> findAllByPlayerProfileIdOrderByCreatedAtDesc(Long playerProfileId);
}
