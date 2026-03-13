package com.sentimospadel.backend.onboarding.controller;

import com.sentimospadel.backend.onboarding.dto.InitialSurveyRequest;
import com.sentimospadel.backend.onboarding.dto.InitialSurveyResponse;
import com.sentimospadel.backend.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping("/initial-survey")
    public ResponseEntity<InitialSurveyResponse> submitInitialSurvey(
            Authentication authentication,
            @Valid @RequestBody InitialSurveyRequest request
    ) {
        InitialSurveyResponse response = onboardingService.submitInitialSurvey(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/onboarding/initial-survey")).body(response);
    }

    @GetMapping("/initial-survey")
    public InitialSurveyResponse getInitialSurvey(Authentication authentication) {
        return onboardingService.getInitialSurvey(authentication.getName());
    }
}
