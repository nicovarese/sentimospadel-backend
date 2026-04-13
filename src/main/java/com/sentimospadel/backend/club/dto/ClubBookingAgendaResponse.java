package com.sentimospadel.backend.club.dto;

import com.sentimospadel.backend.club.enums.ClubBookingMode;
import java.time.LocalDate;
import java.util.List;

public record ClubBookingAgendaResponse(
        Long clubId,
        String clubName,
        ClubBookingMode bookingMode,
        LocalDate date,
        List<ClubBookingCourtResponse> courts
) {
}
