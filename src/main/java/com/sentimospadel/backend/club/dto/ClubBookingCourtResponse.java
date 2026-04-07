package com.sentimospadel.backend.club.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClubBookingCourtResponse(
        Long id,
        String name,
        BigDecimal hourlyRateUyu,
        List<ClubBookingSlotResponse> slots
) {
}
