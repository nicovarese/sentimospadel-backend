package com.sentimospadel.backend.club.repository;

import com.sentimospadel.backend.club.entity.ClubAgendaSlotOverride;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubAgendaSlotOverrideRepository extends JpaRepository<ClubAgendaSlotOverride, Long> {

    @EntityGraph(attributePaths = {"club", "court"})
    List<ClubAgendaSlotOverride> findAllByClubIdAndSlotDate(Long clubId, LocalDate slotDate);

    @EntityGraph(attributePaths = {"club", "court"})
    List<ClubAgendaSlotOverride> findAllByClubIdAndSlotDateBetween(Long clubId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"club", "court"})
    List<ClubAgendaSlotOverride> findAllByClubIdAndCourtIdAndSlotDateGreaterThanEqual(Long clubId, Long courtId, LocalDate from);

    @EntityGraph(attributePaths = {"club", "court"})
    Optional<ClubAgendaSlotOverride> findByClubIdAndCourtIdAndSlotDateAndStartTime(
            Long clubId,
            Long courtId,
            LocalDate slotDate,
            LocalTime startTime
    );
}
