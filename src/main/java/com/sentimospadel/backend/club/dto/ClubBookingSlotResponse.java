package com.sentimospadel.backend.club.dto;

import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;

public record ClubBookingSlotResponse(
        String time,
        ClubAgendaSlotStatus status
) {
}
