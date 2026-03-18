package com.sentimospadel.backend.onboarding.service;

import com.sentimospadel.backend.onboarding.dto.InitialSurveyRequest;
import com.sentimospadel.backend.onboarding.dto.InitialSurveyResponse;
import com.sentimospadel.backend.onboarding.entity.InitialSurveySubmission;
import com.sentimospadel.backend.onboarding.repository.InitialSurveySubmissionRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.shared.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final int CURRENT_SURVEY_VERSION = 1;

    private final PlayerProfileResolverService playerProfileResolverService;
    private final PlayerProfileRepository playerProfileRepository;
    private final InitialSurveySubmissionRepository initialSurveySubmissionRepository;
    private final InitialSurveyCalculationService initialSurveyCalculationService;

    @Transactional
    public InitialSurveyResponse submitInitialSurvey(String email, InitialSurveyRequest request) {
        // Onboarding stays separate from register, so the authenticated user is resolved here and linked to a profile.
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        if (playerProfile.isSurveyCompleted()) {
            throw new DuplicateResourceException("Initial survey has already been submitted for this player");
        }

        InitialSurveyCalculationResult result = initialSurveyCalculationService.calculate(request);

        InitialSurveySubmission submission = InitialSurveySubmission.builder()
                .playerProfile(playerProfile)
                .surveyVersion(CURRENT_SURVEY_VERSION)
                .q1(request.q1())
                .q2(request.q2())
                .q3(request.q3())
                .q4(request.q4())
                .q5(request.q5())
                .q6(request.q6())
                .q7(request.q7())
                .q8(request.q8())
                .q9(request.q9())
                .q10(request.q10())
                .weightedScore(result.weightedScore())
                .normalizedScore(result.normalizedScore())
                .initialRating(result.initialRating())
                .estimatedCategory(result.estimatedCategory())
                .requiresClubVerification(result.requiresClubVerification())
                .build();

        InitialSurveySubmission savedSubmission = initialSurveySubmissionRepository.save(submission);

        // Mirror the current onboarding result on player_profiles so future reads do not need to derive it from history.
        playerProfile.setSurveyCompleted(true);
        playerProfile.setSurveyCompletedAt(savedSubmission.getCreatedAt());
        playerProfile.setInitialRating(result.initialRating());
        playerProfile.setCurrentRating(result.initialRating());
        playerProfile.setEstimatedCategory(result.estimatedCategory());
        playerProfile.setRequiresClubVerification(result.requiresClubVerification());
        playerProfile.setClubVerificationStatus(result.clubVerificationStatus());
        playerProfileRepository.save(playerProfile);

        return toResponse(savedSubmission, playerProfile);
    }

    @Transactional(readOnly = true)
    public InitialSurveyResponse getInitialSurvey(String email) {
        PlayerProfile playerProfile = playerProfileRepository.findByUserId(
                        playerProfileResolverService.getUserByEmail(email).getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Player profile for the authenticated user was not found"));

        InitialSurveySubmission submission = initialSurveySubmissionRepository
                .findTopByPlayerProfileIdOrderByCreatedAtDesc(playerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Initial survey result for the authenticated user was not found"));

        return toResponse(submission, playerProfile);
    }

    private InitialSurveyResponse toResponse(InitialSurveySubmission submission, PlayerProfile playerProfile) {
        return new InitialSurveyResponse(
                submission.getId(),
                submission.getSurveyVersion(),
                submission.getQ1(),
                submission.getQ2(),
                submission.getQ3(),
                submission.getQ4(),
                submission.getQ5(),
                submission.getQ6(),
                submission.getQ7(),
                submission.getQ8(),
                submission.getQ9(),
                submission.getQ10(),
                submission.getWeightedScore(),
                submission.getNormalizedScore(),
                submission.getInitialRating(),
                submission.getEstimatedCategory(),
                submission.isRequiresClubVerification(),
                playerProfile.getClubVerificationStatus(),
                submission.getCreatedAt(),
                submission.getUpdatedAt()
        );
    }
}
