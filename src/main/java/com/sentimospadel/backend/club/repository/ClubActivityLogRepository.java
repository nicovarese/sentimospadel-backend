package com.sentimospadel.backend.club.repository;

import com.sentimospadel.backend.club.entity.ClubActivityLog;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubActivityLogRepository extends JpaRepository<ClubActivityLog, Long> {

    @EntityGraph(attributePaths = {"club"})
    List<ClubActivityLog> findTop8ByClubIdOrderByOccurredAtDesc(Long clubId);
}
