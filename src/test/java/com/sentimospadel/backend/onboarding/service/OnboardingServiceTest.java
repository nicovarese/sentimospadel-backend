package com.sentimospadel.backend.onboarding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.onboarding.dto.InitialSurveyRequest;
import com.sentimospadel.backend.onboarding.dto.InitialSurveyResponse;
import com.sentimospadel.backend.onboarding.entity.InitialSurveySubmission;
import com.sentimospadel.backend.onboarding.enums.AnswerOption;
import com.sentimospadel.backend.onboarding.repository.InitialSurveySubmissionRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.enums.UruguayCategory;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private InitialSurveySubmissionRepository initialSurveySubmissionRepository;

    @Mock
    private InitialSurveyCalculationService initialSurveyCalculationService;

    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(
                playerProfileResolverService,
                playerProfileRepository,
                initialSurveySubmissionRepository,
                initialSurveyCalculationService
        );
    }

    @Test
    void submitInitialSurveyCreatesMissingPlayerProfileAndPersistsCurrentResult() {
        PlayerProfile playerProfile = PlayerProfile.builder()
                .fullName("Player")
                .currentRating(new BigDecimal("1.00"))
                .provisional(true)
                .matchesPlayed(0)
                .ratedMatchesCount(0)
                .build();
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(playerProfile);
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InitialSurveyCalculationResult calculationResult = new InitialSurveyCalculationResult(
                126,
                new BigDecimal("31.50"),
                new BigDecimal("5.52"),
                UruguayCategory.SEGUNDA,
                true,
                ClubVerificationStatus.PENDING
        );

        when(initialSurveyCalculationService.calculate(any(InitialSurveyRequest.class))).thenReturn(calculationResult);
        when(initialSurveySubmissionRepository.save(any(InitialSurveySubmission.class))).thenAnswer(invocation -> {
            InitialSurveySubmission submission = invocation.getArgument(0);
            submission.setCreatedAt(Instant.parse("2026-03-13T14:00:00Z"));
            submission.setUpdatedAt(Instant.parse("2026-03-13T14:00:00Z"));
            return submission;
        });

        InitialSurveyRequest request = new InitialSurveyRequest(
                AnswerOption.E,
                AnswerOption.D,
                AnswerOption.D,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.D,
                AnswerOption.C,
                AnswerOption.C,
                AnswerOption.B,
                AnswerOption.D
        );

        InitialSurveyResponse response = onboardingService.submitInitialSurvey("player@example.com", request);

        ArgumentCaptor<PlayerProfile> profileCaptor = ArgumentCaptor.forClass(PlayerProfile.class);
        verify(playerProfileRepository).save(profileCaptor.capture());
        PlayerProfile persistedProfile = profileCaptor.getValue();

        assertTrue(persistedProfile.isSurveyCompleted());
        assertEquals(ClubVerificationStatus.PENDING, persistedProfile.getClubVerificationStatus());
        assertEquals(new BigDecimal("5.52"), persistedProfile.getInitialRating());
        assertEquals(new BigDecimal("5.52"), persistedProfile.getCurrentRating());
        assertEquals(UruguayCategory.SEGUNDA, persistedProfile.getEstimatedCategory());
        assertEquals("Player", persistedProfile.getFullName());

        assertEquals(new BigDecimal("5.52"), response.initialRating());
        assertEquals(UruguayCategory.SEGUNDA, response.estimatedCategory());
        assertTrue(response.requiresClubVerification());
        assertEquals(ClubVerificationStatus.PENDING, response.clubVerificationStatus());
    }
}
