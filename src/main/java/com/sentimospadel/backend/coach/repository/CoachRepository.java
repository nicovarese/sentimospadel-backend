package com.sentimospadel.backend.coach.repository;

import com.sentimospadel.backend.coach.entity.Coach;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachRepository extends JpaRepository<Coach, Long> {

    List<Coach> findAllByActiveTrueOrderByAverageRatingDescFullNameAsc();
}
