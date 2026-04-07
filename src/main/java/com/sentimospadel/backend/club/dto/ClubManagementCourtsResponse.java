package com.sentimospadel.backend.club.dto;

import java.util.List;

public record ClubManagementCourtsResponse(
        Long clubId,
        String clubName,
        Integer activeCourtsCount,
        Integer totalCourtsCount,
        List<ClubManagementCourtResponse> courts
) {
}
