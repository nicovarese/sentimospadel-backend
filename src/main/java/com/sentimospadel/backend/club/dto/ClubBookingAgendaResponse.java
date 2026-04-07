package com.sentimospadel.backend.club.dto;

import java.time.LocalDate;
import java.util.List;

public record ClubBookingAgendaResponse(
        Long clubId,
        String clubName,
        LocalDate date,
        List<ClubBookingCourtResponse> courts
) {
}
