package com.sentimospadel.backend.club.service;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.enums.ClubBookingMode;

public record ClubBookingResolution(
        Club club,
        ClubBookingMode bookingMode
) {
}
