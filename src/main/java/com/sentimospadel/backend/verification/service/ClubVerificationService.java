package com.sentimospadel.backend.verification.service;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.entity.ClubActivityLog;
import com.sentimospadel.backend.club.repository.ClubActivityLogRepository;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.notification.service.PlayerEventNotificationService;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.verification.dto.ClubVerificationDecisionRequest;
import com.sentimospadel.backend.verification.dto.ClubVerificationManagementRequestResponse;
import com.sentimospadel.backend.verification.dto.CreateClubVerificationRequest;
import com.sentimospadel.backend.verification.dto.PlayerClubVerificationRequestResponse;
import com.sentimospadel.backend.verification.dto.PlayerClubVerificationSummaryResponse;
import com.sentimospadel.backend.verification.entity.ClubVerificationRequest;
import com.sentimospadel.backend.verification.enums.ClubVerificationRequestStatus;
import com.sentimospadel.backend.verification.repository.ClubVerificationRequestRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubVerificationService {

    private final PlayerProfileResolverService playerProfileResolverService;
    private final PlayerProfileRepository playerProfileRepository;
    private final ClubRepository clubRepository;
    private final ClubActivityLogRepository clubActivityLogRepository;
    private final ClubVerificationRequestRepository clubVerificationRequestRepository;
    private final PlayerEventNotificationService playerEventNotificationService;

    @Transactional(readOnly = true)
    public PlayerClubVerificationSummaryResponse getMyVerificationSummary(String authenticatedEmail) {
        PlayerProfile playerProfile = findPlayerProfile(authenticatedEmail);

        if (playerProfile == null) {
            return new PlayerClubVerificationSummaryResponse(
                    false,
                    ClubVerificationStatus.NOT_REQUIRED,
                    false,
                    List.of()
            );
        }

        return toPlayerSummary(playerProfile);
    }

    @Transactional
    public PlayerClubVerificationSummaryResponse createMyVerificationRequest(
            String authenticatedEmail,
            CreateClubVerificationRequest request
    ) {
        PlayerProfile playerProfile = requirePlayerProfile(authenticatedEmail);
        validatePlayerCanRequest(playerProfile);

        if (clubVerificationRequestRepository.existsByPlayerProfileIdAndStatus(
                playerProfile.getId(),
                ClubVerificationRequestStatus.PENDING
        )) {
            throw new ConflictException("There is already a pending club verification request for this player");
        }

        Club club = clubRepository.findById(request.clubId())
                .orElseThrow(() -> new ResourceNotFoundException("Club with id " + request.clubId() + " was not found"));

        clubVerificationRequestRepository.save(ClubVerificationRequest.builder()
                .playerProfile(playerProfile)
                .club(club)
                .status(ClubVerificationRequestStatus.PENDING)
                .build());

        playerProfile.setClubVerificationStatus(ClubVerificationStatus.PENDING);
        registerActivity(club, "Solicitud de verificacion", playerProfile.getFullName() + " solicito validacion de categoria");

        return toPlayerSummary(playerProfile);
    }

    @Transactional(readOnly = true)
    public List<ClubVerificationManagementRequestResponse> getManagedClubRequests(String authenticatedEmail) {
        Club managedClub = resolveManagedClub(authenticatedEmail);

        return clubVerificationRequestRepository.findAllByClubIdOrderByCreatedAtDesc(managedClub.getId())
                .stream()
                .sorted(Comparator
                        .comparing((ClubVerificationRequest request) -> request.getStatus() != ClubVerificationRequestStatus.PENDING)
                        .thenComparing(ClubVerificationRequest::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toManagementResponse)
                .toList();
    }

    @Transactional
    public ClubVerificationManagementRequestResponse approveManagedClubRequest(
            String authenticatedEmail,
            Long requestId,
            ClubVerificationDecisionRequest request
    ) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        User reviewer = playerProfileResolverService.getUserByEmail(authenticatedEmail);
        ClubVerificationRequest verificationRequest = requireManagedClubRequest(managedClub.getId(), requestId);

        if (verificationRequest.getStatus() != ClubVerificationRequestStatus.PENDING) {
            throw new ConflictException("Only pending club verification requests can be approved");
        }

        PlayerProfile playerProfile = verificationRequest.getPlayerProfile();
        if (!playerProfile.isRequiresClubVerification()) {
            throw new ConflictException("This player does not require club verification");
        }

        verificationRequest.setStatus(ClubVerificationRequestStatus.APPROVED);
        verificationRequest.setReviewedAt(Instant.now());
        verificationRequest.setReviewedByUser(reviewer);
        verificationRequest.setReviewNotes(trimToNull(request.notes()));
        playerProfile.setClubVerificationStatus(ClubVerificationStatus.VERIFIED);

        registerActivity(managedClub, "Verificacion aprobada", playerProfile.getFullName() + " fue validado por el club");
        playerEventNotificationService.notifyClubVerificationDecision(verificationRequest);
        return toManagementResponse(verificationRequest);
    }

    @Transactional
    public ClubVerificationManagementRequestResponse rejectManagedClubRequest(
            String authenticatedEmail,
            Long requestId,
            ClubVerificationDecisionRequest request
    ) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        User reviewer = playerProfileResolverService.getUserByEmail(authenticatedEmail);
        ClubVerificationRequest verificationRequest = requireManagedClubRequest(managedClub.getId(), requestId);

        if (verificationRequest.getStatus() != ClubVerificationRequestStatus.PENDING) {
            throw new ConflictException("Only pending club verification requests can be rejected");
        }

        PlayerProfile playerProfile = verificationRequest.getPlayerProfile();
        if (!playerProfile.isRequiresClubVerification()) {
            throw new ConflictException("This player does not require club verification");
        }

        verificationRequest.setStatus(ClubVerificationRequestStatus.REJECTED);
        verificationRequest.setReviewedAt(Instant.now());
        verificationRequest.setReviewedByUser(reviewer);
        verificationRequest.setReviewNotes(trimToNull(request.notes()));
        playerProfile.setClubVerificationStatus(ClubVerificationStatus.REJECTED);

        registerActivity(managedClub, "Verificacion rechazada", playerProfile.getFullName() + " requiere nueva revision");
        playerEventNotificationService.notifyClubVerificationDecision(verificationRequest);
        return toManagementResponse(verificationRequest);
    }

    private void validatePlayerCanRequest(PlayerProfile playerProfile) {
        if (!playerProfile.isSurveyCompleted()) {
            throw new ConflictException("Club verification can only be requested after onboarding is completed");
        }

        if (!playerProfile.isRequiresClubVerification()) {
            throw new ConflictException("This player does not require club verification");
        }

        if (playerProfile.getClubVerificationStatus() == ClubVerificationStatus.VERIFIED) {
            throw new ConflictException("This player is already club-verified");
        }
    }

    private PlayerClubVerificationSummaryResponse toPlayerSummary(PlayerProfile playerProfile) {
        List<PlayerClubVerificationRequestResponse> requests = clubVerificationRequestRepository
                .findTop5ByPlayerProfileIdOrderByCreatedAtDesc(playerProfile.getId())
                .stream()
                .map(this::toPlayerRequestResponse)
                .toList();

        boolean hasPendingRequest = requests.stream()
                .anyMatch(request -> request.status() == ClubVerificationRequestStatus.PENDING);

        return new PlayerClubVerificationSummaryResponse(
                playerProfile.isRequiresClubVerification(),
                playerProfile.getClubVerificationStatus(),
                playerProfile.isRequiresClubVerification()
                        && playerProfile.getClubVerificationStatus() != ClubVerificationStatus.VERIFIED
                        && !hasPendingRequest,
                requests
        );
    }

    private PlayerClubVerificationRequestResponse toPlayerRequestResponse(ClubVerificationRequest request) {
        return new PlayerClubVerificationRequestResponse(
                request.getId(),
                request.getClub().getId(),
                request.getClub().getName(),
                request.getClub().getCity(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getReviewedAt(),
                request.getReviewNotes()
        );
    }

    private ClubVerificationManagementRequestResponse toManagementResponse(ClubVerificationRequest request) {
        PlayerProfile playerProfile = request.getPlayerProfile();
        UruguayCategory currentCategory = UruguayCategoryMapper.fromRating(playerProfile.getCurrentRating());

        return new ClubVerificationManagementRequestResponse(
                request.getId(),
                playerProfile.getId(),
                playerProfile.getFullName(),
                playerProfile.getPhotoUrl(),
                playerProfile.getCity(),
                playerProfile.getCurrentRating(),
                currentCategory,
                request.getCreatedAt(),
                request.getStatus(),
                request.getReviewedAt(),
                request.getReviewNotes()
        );
    }

    private ClubVerificationRequest requireManagedClubRequest(Long managedClubId, Long requestId) {
        ClubVerificationRequest verificationRequest = clubVerificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Club verification request with id " + requestId + " was not found"));

        if (!verificationRequest.getClub().getId().equals(managedClubId)) {
            throw new AccessDeniedException("This verification request does not belong to the managed club");
        }

        return verificationRequest;
    }

    private PlayerProfile requirePlayerProfile(String authenticatedEmail) {
        PlayerProfile playerProfile = findPlayerProfile(authenticatedEmail);
        if (playerProfile == null) {
            throw new ResourceNotFoundException("Player profile for the authenticated user was not found");
        }
        return playerProfile;
    }

    private PlayerProfile findPlayerProfile(String authenticatedEmail) {
        User user = playerProfileResolverService.getUserByEmail(authenticatedEmail);
        return playerProfileRepository.findByUserId(user.getId()).orElse(null);
    }

    private Club resolveManagedClub(String authenticatedEmail) {
        User user = playerProfileResolverService.getUserByEmail(authenticatedEmail);

        if (user.getRole() != UserRole.ADMIN || user.getManagedClub() == null) {
            throw new AccessDeniedException("Only club administrators with a managed club can access this resource");
        }

        return user.getManagedClub();
    }

    private void registerActivity(Club club, String title, String description) {
        clubActivityLogRepository.save(ClubActivityLog.builder()
                .club(club)
                .title(title)
                .description(description)
                .occurredAt(Instant.now())
                .build());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
