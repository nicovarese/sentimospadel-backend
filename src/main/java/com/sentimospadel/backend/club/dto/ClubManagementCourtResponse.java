package com.sentimospadel.backend.club.dto;

import java.math.BigDecimal;

public record ClubManagementCourtResponse(
        Long id,
        String name,
        Integer displayOrder,
        BigDecimal hourlyRateUyu,
        boolean active
) {
}
