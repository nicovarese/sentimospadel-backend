package com.sentimospadel.backend.auth.dto;

import com.sentimospadel.backend.auth.enums.RegisterAccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,
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
        String clubAddress
) {
}
