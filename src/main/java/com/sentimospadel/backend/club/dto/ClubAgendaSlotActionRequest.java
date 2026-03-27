package com.sentimospadel.backend.club.dto;

import com.sentimospadel.backend.club.enums.ClubAgendaSlotActionType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ClubAgendaSlotActionRequest(
        @NotNull LocalDate date,
        @NotNull Long courtId,
        @NotNull LocalTime time,
        @NotNull ClubAgendaSlotActionType action,
        String reservedByName
) {
}
