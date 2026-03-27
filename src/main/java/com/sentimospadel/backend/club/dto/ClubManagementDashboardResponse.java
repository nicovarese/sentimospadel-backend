package com.sentimospadel.backend.club.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClubManagementDashboardResponse(
        Long clubId,
        String clubName,
        Integer activeCourtsCount,
        Integer totalCourtsCount,
        BigDecimal todayRevenueUyu,
        Integer todayReservationsCount,
        List<ClubManagementActivityResponse> recentActivities
) {
}
