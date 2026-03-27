package com.sentimospadel.backend.club.dto;

import com.sentimospadel.backend.club.enums.ClubQuickActionType;
import jakarta.validation.constraints.NotNull;

public record ClubQuickActionRequest(
        @NotNull ClubQuickActionType type
) {
}
