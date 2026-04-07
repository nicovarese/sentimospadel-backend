package com.sentimospadel.backend.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubActivityLogRepository;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.verification.dto.ClubVerificationDecisionRequest;
import com.sentimospadel.backend.verification.dto.ClubVerificationManagementRequestResponse;
import com.sentimospadel.backend.verification.dto.CreateClubVerificationRequest;
import com.sentimospadel.backend.verification.dto.PlayerClubVerificationSummaryResponse;
import com.sentimospadel.backend.verification.entity.ClubVerificationRequest;
import com.sentimospadel.backend.verification.enums.ClubVerificationRequestStatus;
import com.sentimospadel.backend.verification.repository.ClubVerificationRequestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ClubVerificationServiceTest {

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubActivityLogRepository clubActivityLogRepository;

    @Mock
    private ClubVerificationRequestRepository clubVerificationRequestRepository;

    private ClubVerificationService clubVerificationService;

    @BeforeEach
    void setUp() {
        clubVerificationService = new ClubVerificationService(
                playerProfileResolverService,
                playerProfileRepository,
                clubRepository,
                clubActivityLogRepository,
                clubVerificationRequestRepository
        );
    }

    @Test
    void createMyVerificationRequestCreatesPendingRequestAndLocksNewRequests() {
        User playerUser = User.builder()
                .id(10L)
                .email("player@example.com")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
        PlayerProfile playerProfile = PlayerProfile.builder()
                .id(100L)
                .user(playerUser)
                .fullName("Player One")
                .currentRating(new BigDecimal("5.85"))
                .surveyCompleted(true)
                .requiresClubVerification(true)
                .clubVerificationStatus(ClubVerificationStatus.REJECTED)
                .build();
        Club club = Club.builder()
                .id(1L)
                .name("Top Padel")
                .city("Montevideo")
                .build();
        ClubVerificationRequest pendingRequest = ClubVerificationRequest.builder()
                .id(50L)
                .playerProfile(playerProfile)
                .club(club)
                .status(ClubVerificationRequestStatus.PENDING)
                .build();
        pendingRequest.setCreatedAt(Instant.parse("2026-03-31T13:00:00Z"));
        pendingRequest.setUpdatedAt(Instant.parse("2026-03-31T13:00:00Z"));

        when(playerProfileResolverService.getUserByEmail("player@example.com")).thenReturn(playerUser);
        when(playerProfileRepository.findByUserId(10L)).thenReturn(Optional.of(playerProfile));
        when(clubVerificationRequestRepository.existsByPlayerProfileIdAndStatus(100L, ClubVerificationRequestStatus.PENDING))
                .thenReturn(false);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(clubVerificationRequestRepository.save(any())).thenReturn(pendingRequest);
        when(clubVerificationRequestRepository.findTop5ByPlayerProfileIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(pendingRequest));

        PlayerClubVerificationSummaryResponse response = clubVerificationService.createMyVerificationRequest(
                "player@example.com",
                new CreateClubVerificationRequest(1L)
        );

        assertThat(playerProfile.getClubVerificationStatus()).isEqualTo(ClubVerificationStatus.PENDING);
        assertThat(response.canCreateRequest()).isFalse();
        assertThat(response.requests()).hasSize(1);
        assertThat(response.requests().get(0).clubName()).isEqualTo("Top Padel");
        verify(clubActivityLogRepository).save(any());
    }

    @Test
    void createMyVerificationRequestRejectsPlayersWithoutVerificationRequirement() {
        User playerUser = User.builder()
                .id(10L)
                .email("player@example.com")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
        PlayerProfile playerProfile = PlayerProfile.builder()
                .id(100L)
                .user(playerUser)
                .fullName("Player One")
                .currentRating(new BigDecimal("4.20"))
                .surveyCompleted(true)
                .requiresClubVerification(false)
                .clubVerificationStatus(ClubVerificationStatus.NOT_REQUIRED)
                .build();

        when(playerProfileResolverService.getUserByEmail("player@example.com")).thenReturn(playerUser);
        when(playerProfileRepository.findByUserId(10L)).thenReturn(Optional.of(playerProfile));

        assertThatThrownBy(() -> clubVerificationService.createMyVerificationRequest(
                "player@example.com",
                new CreateClubVerificationRequest(1L)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("does not require club verification");
    }

    @Test
    void approveManagedClubRequestMarksPlayerAsVerified() {
        Club club = Club.builder()
                .id(1L)
                .name("Top Padel")
                .city("Montevideo")
                .build();
        User adminUser = User.builder()
                .id(1L)
                .email("club.admin@sentimospadel.test")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .managedClub(club)
                .build();
        User playerUser = User.builder()
                .id(10L)
                .email("player@example.com")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();
        PlayerProfile playerProfile = PlayerProfile.builder()
                .id(100L)
                .user(playerUser)
                .fullName("Player One")
                .city("Montevideo")
                .currentRating(new BigDecimal("5.90"))
                .surveyCompleted(true)
                .requiresClubVerification(true)
                .clubVerificationStatus(ClubVerificationStatus.PENDING)
                .build();
        ClubVerificationRequest pendingRequest = ClubVerificationRequest.builder()
                .id(50L)
                .playerProfile(playerProfile)
                .club(club)
                .status(ClubVerificationRequestStatus.PENDING)
                .build();
        pendingRequest.setCreatedAt(Instant.parse("2026-03-31T13:00:00Z"));
        pendingRequest.setUpdatedAt(Instant.parse("2026-03-31T13:00:00Z"));

        when(playerProfileResolverService.getUserByEmail("club.admin@sentimospadel.test")).thenReturn(adminUser);
        when(clubVerificationRequestRepository.findById(50L)).thenReturn(Optional.of(pendingRequest));

        ClubVerificationManagementRequestResponse response = clubVerificationService.approveManagedClubRequest(
                "club.admin@sentimospadel.test",
                50L,
                new ClubVerificationDecisionRequest("Nivel validado en competencia del club")
        );

        assertThat(playerProfile.getClubVerificationStatus()).isEqualTo(ClubVerificationStatus.VERIFIED);
        assertThat(response.status()).isEqualTo(ClubVerificationRequestStatus.APPROVED);
        assertThat(response.currentCategory()).isEqualTo(UruguayCategory.SEGUNDA);
        assertThat(response.reviewNotes()).isEqualTo("Nivel validado en competencia del club");
        verify(clubActivityLogRepository).save(any());
    }

    @Test
    void getManagedClubRequestsRejectsUsersWithoutManagedClubAdminContext() {
        User playerUser = User.builder()
                .id(10L)
                .email("player@example.com")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build();

        when(playerProfileResolverService.getUserByEmail("player@example.com")).thenReturn(playerUser);

        assertThatThrownBy(() -> clubVerificationService.getManagedClubRequests("player@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("club administrators");
    }
}
