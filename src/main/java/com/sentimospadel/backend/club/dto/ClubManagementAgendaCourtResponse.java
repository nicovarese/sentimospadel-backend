package com.sentimospadel.backend.club.dto;

import java.util.List;

public record ClubManagementAgendaCourtResponse(
        Long id,
        String name,
        List<ClubManagementAgendaSlotResponse> slots
) {
}
