package com.sentimospadel.backend.club.dto;

import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;

public record ClubManagementAgendaSlotResponse(
        String id,
        String time,
        ClubAgendaSlotStatus status,
        String reservedByName,
        Long matchId
) {
}
