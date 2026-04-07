package com.sentimospadel.backend.auth.dto;

public record EmailVerificationPageResult(
        boolean success,
        String title,
        String message,
        String actionLabel,
        String actionUrl
) {
}
