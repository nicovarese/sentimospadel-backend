package com.sentimospadel.backend.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.club.dto.ClubAgendaSlotActionRequest;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubManagementCourtsResponse;
import com.sentimospadel.backend.club.dto.ClubQuickActionRequest;
import com.sentimospadel.backend.club.dto.CreateClubCourtRequest;
import com.sentimospadel.backend.club.dto.ReorderClubCourtsRequest;
import com.sentimospadel.backend.club.dto.UpdateClubCourtRequest;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.entity.ClubAgendaSlotOverride;
import com.sentimospadel.backend.club.entity.ClubCourt;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotActionType;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.enums.ClubQuickActionType;
import com.sentimospadel.backend.club.repository.ClubActivityLogRepository;
import com.sentimospadel.backend.club.repository.ClubAgendaSlotOverrideRepository;
import com.sentimospadel.backend.club.repository.ClubCourtRepository;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void getCourtsReturnsActiveAndInactiveCourtsOrdered() {
        Club club = Club.builder().id(1L).name("Top Padel").city("Montevideo").build();
        User adminUser = User.builder()
                .id(1L)
                .email("club.admin@sentimospadel.test")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .managedClub(club)
                .build();
        ClubCourt activeCourt = ClubCourt.builder()
                .id(7L)
                .club(club)
                .name("Cancha 1")
                .displayOrder(1)
                .hourlyRateUyu(BigDecimal.valueOf(1200))
                .active(true)
                .build();
        ClubCourt inactiveCourt = ClubCourt.builder()
                .id(8L)
                .club(club)
                .name("Cancha 2")
                .displayOrder(2)
                .hourlyRateUyu(BigDecimal.valueOf(1100))
                .active(false)
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));
        when(clubCourtRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of(activeCourt, inactiveCourt));

        ClubManagementCourtsResponse response = clubManagementService.getCourts("club.admin@sentimospadel.test");

        assertThat(response.activeCourtsCount()).isEqualTo(1);
        assertThat(response.totalCourtsCount()).isEqualTo(2);
        assertThat(response.courts()).extracting(court -> court.name() + ":" + court.active())
                .containsExactly("Cancha 1:true", "Cancha 2:false");
    }

    @Test
    void createCourtAppendsNewCourtAtTheEndOfTheOrder() {
        Club club = Club.builder().id(1L).name("Top Padel").city("Montevideo").build();
        User adminUser = User.builder()
                .id(1L)
                .email("club.admin@sentimospadel.test")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .managedClub(club)
                .build();
        ClubCourt existingCourt = ClubCourt.builder()
                .id(7L)
                .club(club)
                .name("Cancha 1")
                .displayOrder(1)
                .hourlyRateUyu(BigDecimal.valueOf(1200))
                .active(true)
                .build();
        ClubCourt createdCourt = ClubCourt.builder()
                .id(8L)
                .club(club)
                .name("Cancha 2")
                .displayOrder(2)
                .hourlyRateUyu(BigDecimal.valueOf(1150))
                .active(true)
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));
        when(clubCourtRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(existingCourt))
                .thenReturn(List.of(existingCourt, createdCourt));
        when(clubCourtRepository.save(any(ClubCourt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClubManagementCourtsResponse response = clubManagementService.createCourt(
                "club.admin@sentimospadel.test",
                new CreateClubCourtRequest("Cancha 2", BigDecimal.valueOf(1150))
        );

        ArgumentCaptor<ClubCourt> courtCaptor = ArgumentCaptor.forClass(ClubCourt.class);
        verify(clubCourtRepository).save(courtCaptor.capture());
        assertThat(courtCaptor.getValue().getDisplayOrder()).isEqualTo(2);
        assertThat(courtCaptor.getValue().isActive()).isTrue();
        assertThat(response.totalCourtsCount()).isEqualTo(2);
        assertThat(response.courts()).extracting(court -> court.name()).containsExactly("Cancha 1", "Cancha 2");
    }

    @Test
    void updateCourtRejectsDeactivationWhenFutureOverridesExist() {
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
                .name("Cancha 1")
                .displayOrder(1)
                .hourlyRateUyu(BigDecimal.valueOf(1200))
                .active(true)
                .build();
        ClubAgendaSlotOverride futureOverride = ClubAgendaSlotOverride.builder()
                .id(20L)
                .club(club)
                .court(court)
                .slotDate(LocalDate.now(ZoneId.of("America/Montevideo")).plusDays(1))
                .startTime(LocalTime.of(19, 0))
                .status(ClubAgendaSlotStatus.RESERVED)
                .reservedByName("Reserva futura")
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));
        when(clubCourtRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of(court));
        when(clubAgendaSlotOverrideRepository.findAllByClubIdAndCourtIdAndSlotDateGreaterThanEqual(eq(1L), eq(7L), any(LocalDate.class)))
                .thenReturn(List.of(futureOverride));

        assertThatThrownBy(() -> clubManagementService.updateCourt(
                "club.admin@sentimospadel.test",
                7L,
                new UpdateClubCourtRequest("Cancha 1", BigDecimal.valueOf(1200), false)
        ))
                .isInstanceOf(com.sentimospadel.backend.shared.exception.ConflictException.class)
                .hasMessageContaining("cannot be deactivated");
    }

    @Test
    void updateCourtRejectsRenameWhenFutureRealMatchesExist() {
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
        Match futureMatch = Match.builder()
                .id(30L)
                .status(MatchStatus.OPEN)
                .scheduledAt(Instant.now().plusSeconds(86400))
                .club(club)
                .locationText("Top Padel - Cancha 1 (Cristal)")
                .maxPlayers(4)
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));
        when(clubCourtRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of(court));
        when(matchRepository.findAllByClubIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(eq(1L), any(Instant.class)))
                .thenReturn(List.of(futureMatch));

        assertThatThrownBy(() -> clubManagementService.updateCourt(
                "club.admin@sentimospadel.test",
                7L,
                new UpdateClubCourtRequest("Cancha Central", BigDecimal.valueOf(1200), true)
        ))
                .isInstanceOf(com.sentimospadel.backend.shared.exception.ConflictException.class)
                .hasMessageContaining("cannot be renamed");
    }

    @Test
    void reorderCourtsUpdatesDisplayOrder() {
        Club club = Club.builder().id(1L).name("Top Padel").city("Montevideo").build();
        User adminUser = User.builder()
                .id(1L)
                .email("club.admin@sentimospadel.test")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .managedClub(club)
                .build();
        ClubCourt courtOne = ClubCourt.builder()
                .id(7L)
                .club(club)
                .name("Cancha 1")
                .displayOrder(1)
                .hourlyRateUyu(BigDecimal.valueOf(1200))
                .active(true)
                .build();
        ClubCourt courtTwo = ClubCourt.builder()
                .id(8L)
                .club(club)
                .name("Cancha 2")
                .displayOrder(2)
                .hourlyRateUyu(BigDecimal.valueOf(1150))
                .active(true)
                .build();

        when(userRepository.findByEmail("club.admin@sentimospadel.test")).thenReturn(Optional.of(adminUser));
        when(clubCourtRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(courtOne, courtTwo))
                .thenReturn(List.of(courtTwo, courtOne));

        ClubManagementCourtsResponse response = clubManagementService.reorderCourts(
                "club.admin@sentimospadel.test",
                new ReorderClubCourtsRequest(List.of(8L, 7L))
        );

        verify(clubCourtRepository, times(1)).saveAll(any());
        assertThat(courtTwo.getDisplayOrder()).isEqualTo(1);
        assertThat(courtOne.getDisplayOrder()).isEqualTo(2);
        assertThat(response.courts()).extracting(court -> court.id()).containsExactly(8L, 7L);
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
        ).isEqualTo("Registro operativo guardado: aviso a usuarios");
    }
}


