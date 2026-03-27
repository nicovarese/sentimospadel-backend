package com.sentimospadel.backend.club.repository;

import com.sentimospadel.backend.club.entity.Club;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {

    List<Club> findAllByOrderByIdAsc();

    List<Club> findByCityIgnoreCase(String city);
}
