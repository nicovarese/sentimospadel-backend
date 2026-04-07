package com.sentimospadel.backend.club.controller;

import com.sentimospadel.backend.club.dto.ClubBookingAgendaResponse;
import com.sentimospadel.backend.club.service.ClubBookingService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubBookingController {

    private final ClubBookingService clubBookingService;

    @GetMapping("/{id}/booking-availability")
    public ClubBookingAgendaResponse getBookingAvailability(
            @PathVariable Long id,
            @RequestParam LocalDate date
    ) {
        return clubBookingService.getBookingAgenda(id, date);
    }
}
