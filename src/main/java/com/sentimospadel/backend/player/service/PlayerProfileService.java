package com.sentimospadel.backend.player.service;

import com.sentimospadel.backend.player.dto.PlayerProfileResponse;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerProfileService {

    private final PlayerProfileRepository playerProfileRepository;

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

    private PlayerProfileResponse toResponse(PlayerProfile playerProfile) {
        return new PlayerProfileResponse(
                playerProfile.getId(),
                playerProfile.getUser().getId(),
                playerProfile.getFullName(),
                playerProfile.getPhotoUrl(),
                playerProfile.getPreferredSide(),
                playerProfile.getDeclaredLevel(),
                playerProfile.getCity(),
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
}
