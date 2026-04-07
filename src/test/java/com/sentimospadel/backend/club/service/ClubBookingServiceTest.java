package com.sentimospadel.backend.club.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.entity.ClubAgendaSlotOverride;
import com.sentimospadel.backend.club.entity.ClubCourt;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.repository.ClubAgendaSlotOverrideRepository;
import com.sentimospadel.backend.club.repository.ClubCourtRepository;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClubBookingServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubCourtRepository clubCourtRepository;

    @Mock
    private ClubAgendaSlotOverrideRepository clubAgendaSlotOverrideRepository;

    @Mock
    private MatchRepository matchRepository;

    private ClubBookingService clubBookingService;

    @BeforeEach
    void setUp() {
        clubBookingService = new ClubBookingService(
                clubRepository,
                clubCourtRepository,
                clubAgendaSlotOverrideRepository,
                matchRepository
        );
    }

    @Test
    void getBookingAgendaReturnsOnlyActiveCourtsAndTheirStatuses() {
        Club club = buildClub(7L, "Top Padel");
        ClubCourt activeCourt = buildCourt(3L, club, "Cancha 3", true, "900.00");
        ClubCourt inactiveCourt = buildCourt(4L, club, "Cancha 4", false, "850.00");

        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubCourtRepository.findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(7L)).thenReturn(List.of(activeCourt));
        when(matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());
        when(clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDate(7L, LocalDate.parse("2026-04-07")))
                .thenReturn(List.of(
                        ClubAgendaSlotOverride.builder()
                                .club(club)
                                .court(activeCourt)
                                .slotDate(LocalDate.parse("2026-04-07"))
                                .startTime(LocalTime.of(20, 30))
                                .status(ClubAgendaSlotStatus.BLOCKED)
                                .build()
                ));

        var response = clubBookingService.getBookingAgenda(7L, LocalDate.parse("2026-04-07"));

        assertEquals(1, response.courts().size());
        assertEquals("Cancha 3", response.courts().get(0).name());
        assertEquals(5, response.courts().get(0).slots().size());
        assertEquals(ClubAgendaSlotStatus.BLOCKED, response.courts().get(0).slots().get(3).status());
    }

    @Test
    void resolveClubBookingRejectsReservedSlot() {
        Club club = buildClub(7L, "Top Padel");
        ClubCourt court = buildCourt(3L, club, "Cancha 3", true, "900.00");
        Match reservedMatch = Match.builder()
                .club(club)
                .status(MatchStatus.OPEN)
                .scheduledAt(Instant.parse("2026-04-07T23:30:00Z"))
                .locationText("Top Padel - Cancha 3")
                .maxPlayers(4)
                .build();

        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubCourtRepository.findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(7L)).thenReturn(List.of(court));
        when(matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of(reservedMatch));
        when(clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDate(7L, LocalDate.parse("2026-04-07")))
                .thenReturn(List.of());

        assertThrows(
                ConflictException.class,
                () -> clubBookingService.resolveClubBooking(
                        7L,
                        Instant.parse("2026-04-07T23:30:00Z"),
                        "Top Padel - Cancha 3"
                )
        );
    }

    @Test
    void resolveClubBookingRejectsBlockedSlot() {
        Club club = buildClub(7L, "Top Padel");
        ClubCourt court = buildCourt(3L, club, "Cancha 3", true, "900.00");
        ClubAgendaSlotOverride blockedOverride = ClubAgendaSlotOverride.builder()
                .club(club)
                .court(court)
                .slotDate(LocalDate.parse("2026-04-07"))
                .startTime(LocalTime.of(20, 30))
                .status(ClubAgendaSlotStatus.BLOCKED)
                .build();

        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubCourtRepository.findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(7L)).thenReturn(List.of(court));
        when(matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());
        when(clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDate(7L, LocalDate.parse("2026-04-07")))
                .thenReturn(List.of(blockedOverride));

        assertThrows(
                ConflictException.class,
                () -> clubBookingService.resolveClubBooking(
                        7L,
                        Instant.parse("2026-04-07T23:30:00Z"),
                        "Top Padel - Cancha 3"
                )
        );
    }

    private Club buildClub(Long id, String name) {
        Club club = Club.builder()
                .name(name)
                .city("Montevideo")
                .integrated(true)
                .build();
        ReflectionTestUtils.setField(club, "id", id);
        return club;
    }

    private ClubCourt buildCourt(Long id, Club club, String name, boolean active, String hourlyRate) {
        ClubCourt court = ClubCourt.builder()
                .club(club)
                .name(name)
                .displayOrder(1)
                .hourlyRateUyu(new BigDecimal(hourlyRate))
                .active(active)
                .build();
        ReflectionTestUtils.setField(court, "id", id);
        return court;
    }
}
