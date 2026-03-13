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
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

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
                userRepository,
                playerProfileRepository,
                initialSurveySubmissionRepository,
                initialSurveyCalculationService
        );
    }

    @Test
    void submitInitialSurveyCreatesMissingPlayerProfileAndPersistsCurrentResult() {
        User user = User.builder()
                .email("player@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
        when(playerProfileRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenAnswer(invocation -> {
            PlayerProfile profile = invocation.getArgument(0);
            if (profile.getId() == null) {
                ReflectionTestUtils.setField(profile, "id", 20L);
            }
            return profile;
        });

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
        verify(playerProfileRepository, org.mockito.Mockito.times(2)).save(profileCaptor.capture());
        PlayerProfile persistedProfile = profileCaptor.getAllValues().get(1);

        assertTrue(persistedProfile.isSurveyCompleted());
        assertEquals(ClubVerificationStatus.PENDING, persistedProfile.getClubVerificationStatus());
        assertEquals(new BigDecimal("5.52"), persistedProfile.getInitialRating());
        assertEquals(UruguayCategory.SEGUNDA, persistedProfile.getEstimatedCategory());
        assertEquals("Player", profileCaptor.getAllValues().get(0).getFullName());

        assertEquals(new BigDecimal("5.52"), response.initialRating());
        assertEquals(UruguayCategory.SEGUNDA, response.estimatedCategory());
        assertTrue(response.requiresClubVerification());
        assertEquals(ClubVerificationStatus.PENDING, response.clubVerificationStatus());
    }
}
