package com.sentimospadel.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.club.dto.ClubAgendaSlotActionRequest;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubQuickActionRequest;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.entity.ClubAgendaSlotOverride;
import com.sentimospadel.backend.club.entity.ClubCourt;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotActionType;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.enums.ClubQuickActionType;
import com.sentimospadel.backend.club.repository.ClubActivityLogRepository;
import com.sentimospadel.backend.club.repository.ClubAgendaSlotOverrideRepository;
import com.sentimospadel.backend.club.repository.ClubCourtRepository;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ClubManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubCourtRepository clubCourtRepository;

    @Mock
    private ClubAgendaSlotOverrideRepository clubAgendaSlotOverrideRepository;

    @Mock
    private ClubActivityLogRepository clubActivityLogRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    private ClubManagementService clubManagementService;

    @BeforeEach
    void setUp() {
        clubManagementService = new ClubManagementService(
                userRepository,
                clubCourtRepository,
                clubAgendaSlotOverrideRepository,
                clubActivityLogRepository,
                matchRepository,
                matchParticipantRepository
        );
    }

    @Test
    void getDashboardRejectsUsersWithoutManagedClubAdminContext() {
        User playerUser = User.builder()
                .id(10L)
                .email("player@example.com")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(playerUser));

        assertThatThrownBy(() -> clubManagementService.getDashboard("player@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("club administrators");
    }

    @Test
    void getAgendaReturnsBackendDrivenSlotsForManagedClub() {
        Club club = Club.builder().id(1L).name("Top Padel").city("Montevideo").build();
        User adminUser = User.builder()
                .id(1L)
                .email("club.admin@sentimospadel.test")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .managedClub(club)
                .build();
        ClubCourt court = ClubCourt.builder()
                .id(7L)
                .club(club)
                .name("Cancha 1 (Cristal)")
                .displayOrder(1)
                .hourlyRateUyu(BigDecimal.valueOf(1200))
                .active(true)
                .build();
        ClubAgendaSlotOverride override = ClubAgendaSlotOverride.builder()
                .id(20L)
                .club(club)
                .court(court)
                .slotDate(LocalDate.of(2026, 3, 26))
                .startTime(LocalTime.of(19, 0))
                .status(ClubAgendaSlotStatus.RESERVED)
                .reservedByName("Reserva Premium")
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));
        when(clubCourtRepository.findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(1L)).thenReturn(List.of(court));
        when(clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDate(1L, LocalDate.of(2026, 3, 26))).thenReturn(List.of(override));
        when(matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());

        ClubManagementAgendaResponse response = clubManagementService.getAgenda(
                "club.admin@sentimospadel.test",
                LocalDate.of(2026, 3, 26)
        );

        assertThat(response.clubName()).isEqualTo("Top Padel");
        assertThat(response.courts()).hasSize(1);
        assertThat(response.courts().get(0).slots())
                .extracting(slot -> slot.time() + ":" + slot.status())
                .contains("19:00:RESERVED");
        assertThat(response.courts().get(0).slots())
                .filteredOn(slot -> slot.time().equals("19:00"))
                .extracting(slot -> slot.reservedByName())
                .containsExactly("Reserva Premium");
    }

    @Test
    void applyAgendaSlotActionFreesManualReservationsAndRegistersActivity() {
        Club club = Club.builder().id(1L).name("Top Padel").city("Montevideo").build();
        User adminUser = User.builder()
                .id(1L)
                .email("club.admin@sentimospadel.test")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .managedClub(club)
                .build();
        ClubCourt court = ClubCourt.builder()
                .id(7L)
                .club(club)
                .name("Cancha 1 (Cristal)")
                .displayOrder(1)
                .hourlyRateUyu(BigDecimal.valueOf(1200))
                .active(true)
                .build();
        ClubAgendaSlotOverride override = ClubAgendaSlotOverride.builder()
                .id(20L)
                .club(club)
                .court(court)
                .slotDate(LocalDate.of(2026, 3, 26))
                .startTime(LocalTime.of(19, 0))
                .status(ClubAgendaSlotStatus.RESERVED)
                .reservedByName("Reserva Premium")
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));
        when(clubCourtRepository.findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(1L)).thenReturn(List.of(court));
        when(clubAgendaSlotOverrideRepository.findByClubIdAndCourtIdAndSlotDateAndStartTime(
                1L,
                7L,
                LocalDate.of(2026, 3, 26),
                LocalTime.of(19, 0)
        )).thenReturn(Optional.of(override));
        when(matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());
        when(clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDate(1L, LocalDate.of(2026, 3, 26))).thenReturn(List.of());

        ClubManagementAgendaResponse response = clubManagementService.applyAgendaSlotAction(
                "club.admin@sentimospadel.test",
                new ClubAgendaSlotActionRequest(
                        LocalDate.of(2026, 3, 26),
                        7L,
                        LocalTime.of(19, 0),
                        ClubAgendaSlotActionType.FREE,
                        null
                )
        );

        verify(clubAgendaSlotOverrideRepository).delete(override);
        verify(clubAgendaSlotOverrideRepository, never()).save(org.mockito.ArgumentMatchers.any());
        assertThat(response.courts().get(0).slots())
                .filteredOn(slot -> slot.time().equals("19:00"))
                .extracting(slot -> slot.status())
                .containsExactly(ClubAgendaSlotStatus.AVAILABLE);
    }

    @Test
    void executeQuickActionReturnsMessage() {
        Club club = Club.builder().id(1L).name("Top Padel").city("Montevideo").build();
        User adminUser = User.builder()
                .id(1L)
                .email("club.admin@sentimospadel.test")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .managedClub(club)
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));

        assertThat(
                clubManagementService.executeQuickAction(
                        "club.admin@sentimospadel.test",
                        new ClubQuickActionRequest(ClubQuickActionType.NOTIFY_USERS)
                ).message()
        ).isEqualTo("Notificación enviada a usuarios activos");
    }
}
