package com.sentimospadel.backend.club.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClubManagementUsersResponse(
        Long clubId,
        String clubName,
        Integer activeUsersCount,
        Integer newUsersThisMonthCount,
        Integer inactiveUsersCount,
        BigDecimal averageRevenuePerUserUyu,
        BigDecimal averageMatchesThisMonth,
        BigDecimal averageMatchesPreviousMonth,
        BigDecimal averageMatchesYear,
        List<ClubManagementTopUserResponse> topUsers
) {
}
