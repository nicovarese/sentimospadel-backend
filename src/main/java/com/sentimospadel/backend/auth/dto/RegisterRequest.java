package com.sentimospadel.backend.auth.dto;

import com.sentimospadel.backend.auth.enums.RegisterAccountType;
import com.sentimospadel.backend.player.enums.PreferredSide;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Size(max = 255, message = "Full name must be at most 255 characters")
        String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,
        @NotBlank(message = "Phone is required")
        @Size(min = 7, max = 40, message = "Phone must be between 7 and 40 characters")
        String phone,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,
        @NotNull(message = "Account type is required")
        RegisterAccountType accountType,
        @Size(max = 255, message = "Club name must be at most 255 characters")
        String clubName,
        @Size(max = 120, message = "Club city must be at most 120 characters")
        String clubCity,
        @Size(max = 255, message = "Club address must be at most 255 characters")
        String clubAddress,
        @Size(max = 500, message = "Photo URL must be at most 500 characters")
        String photoUrl,
        PreferredSide preferredSide,
        @Size(max = 50, message = "Declared level must be at most 50 characters")
        String declaredLevel,
        @Size(max = 120, message = "City must be at most 120 characters")
        String city,
        @Positive(message = "Represented club id must be positive")
        Long representedClubId,
        boolean acceptTerms,
        @Size(max = 40, message = "Accepted terms version must be at most 40 characters")
        String acceptedTermsVersion,
        boolean acceptPrivacyPolicy,
        @Size(max = 40, message = "Accepted privacy version must be at most 40 characters")
        String acceptedPrivacyVersion,
        Boolean allowActivityTracking,
        Boolean allowOperationalNotifications,
        @Size(max = 40, message = "Consent preferences version must be at most 40 characters")
        String consentPreferencesVersion
) {
}
