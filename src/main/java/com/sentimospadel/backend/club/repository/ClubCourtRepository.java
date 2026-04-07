package com.sentimospadel.backend.club.repository;

import com.sentimospadel.backend.club.entity.ClubCourt;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubCourtRepository extends JpaRepository<ClubCourt, Long> {

    @EntityGraph(attributePaths = {"club"})
    List<ClubCourt> findAllByClubIdOrderByDisplayOrderAscIdAsc(Long clubId);

    @EntityGraph(attributePaths = {"club"})
    List<ClubCourt> findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(Long clubId);
}
