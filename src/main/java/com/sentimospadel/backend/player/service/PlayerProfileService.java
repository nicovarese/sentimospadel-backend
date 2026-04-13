package com.sentimospadel.backend.player.service;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.player.dto.PlayerProfileResponse;
import com.sentimospadel.backend.player.dto.UpdatePlayerProfileRequest;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PlayerProfileService {

    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerProfileResolverService playerProfileResolverService;
    private final ClubRepository clubRepository;
    private final PlayerProfilePhotoStorageService playerProfilePhotoStorageService;

    @Transactional(readOnly = true)
    public PlayerProfileResponse getMyPlayerProfile(String email) {
        Long userId = playerProfileResolverService.getUserByEmail(email).getId();

        PlayerProfile playerProfile = playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile for the authenticated user was not found"));

        return toResponse(playerProfile);
    }

    @Transactional(readOnly = true)
    public PlayerProfileResponse getPlayerProfileById(Long id) {
        PlayerProfile playerProfile = playerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile with id " + id + " was not found"));

        return toResponse(playerProfile);
    }

    @Transactional(readOnly = true)
    public List<PlayerProfileResponse> getPlayerProfiles() {
        return playerProfileRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlayerProfileResponse updateMyPlayerProfile(String email, UpdatePlayerProfileRequest request) {
        PlayerProfile playerProfile = getMyPlayerProfileEntity(email);
        String previousPhotoUrl = playerProfile.getPhotoUrl();
        String nextPhotoUrl = trimToNull(request.photoUrl());

        playerProfile.setFullName(request.fullName().trim());
        playerProfile.setPhotoUrl(nextPhotoUrl);
        playerProfile.setPreferredSide(request.preferredSide());
        playerProfile.setDeclaredLevel(request.declaredLevel().trim());
        playerProfile.setCity(request.city().trim());
        playerProfile.setRepresentedClub(resolveRepresentedClub(request.representedClubId()));
        playerProfile.setBio(trimToNull(request.bio()));

        PlayerProfile savedProfile = playerProfileRepository.save(playerProfile);
        if (previousPhotoUrl != null && !previousPhotoUrl.equals(nextPhotoUrl)) {
            playerProfilePhotoStorageService.deleteManagedPhotoIfPresent(previousPhotoUrl);
        }

        return toResponse(savedProfile);
    }

    @Transactional
    public PlayerProfileResponse updateMyPlayerPhoto(String email, MultipartFile file) {
        PlayerProfile playerProfile = getMyPlayerProfileEntity(email);
        String previousPhotoUrl = playerProfile.getPhotoUrl();

        StoredPlayerProfilePhoto storedPhoto = playerProfilePhotoStorageService.store(playerProfile.getId(), file);
        playerProfile.setPhotoUrl(storedPhoto.publicUrl());
        PlayerProfile savedProfile = playerProfileRepository.save(playerProfile);

        if (previousPhotoUrl != null && !previousPhotoUrl.equals(storedPhoto.publicUrl())) {
            playerProfilePhotoStorageService.deleteManagedPhotoIfPresent(previousPhotoUrl);
        }

        return toResponse(savedProfile);
    }

    private PlayerProfileResponse toResponse(PlayerProfile playerProfile) {
        return new PlayerProfileResponse(
                playerProfile.getId(),
                playerProfile.getUser().getId(),
                playerProfile.getFullName(),
                playerProfile.getPhotoUrl(),
                playerProfile.getPreferredSide(),
                playerProfile.getDeclaredLevel(),
                playerProfile.getCity(),
                playerProfile.getRepresentedClub() != null ? playerProfile.getRepresentedClub().getId() : null,
                playerProfile.getRepresentedClub() != null ? playerProfile.getRepresentedClub().getName() : null,
                playerProfile.getBio(),
                playerProfile.getCurrentRating(),
                UruguayCategoryMapper.fromRating(playerProfile.getCurrentRating()),
                playerProfile.isProvisional(),
                playerProfile.getMatchesPlayed(),
                playerProfile.getRatedMatchesCount(),
                playerProfile.isSurveyCompleted(),
                playerProfile.getSurveyCompletedAt(),
                playerProfile.getInitialRating(),
                playerProfile.getEstimatedCategory(),
                playerProfile.isRequiresClubVerification(),
                playerProfile.getClubVerificationStatus(),
                playerProfile.getCreatedAt(),
                playerProfile.getUpdatedAt()
        );
    }

    private Club resolveRepresentedClub(Long representedClubId) {
        if (representedClubId == null) {
            return null;
        }

        return clubRepository.findById(representedClubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club with id " + representedClubId + " was not found"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PlayerProfile getMyPlayerProfileEntity(String email) {
        Long userId = playerProfileResolverService.getUserByEmail(email).getId();

        return playerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile for the authenticated user was not found"));
    }
}
