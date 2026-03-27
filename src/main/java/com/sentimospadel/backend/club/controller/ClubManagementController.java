package com.sentimospadel.backend.club.controller;

import com.sentimospadel.backend.club.dto.ClubAgendaSlotActionRequest;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubManagementDashboardResponse;
import com.sentimospadel.backend.club.dto.ClubManagementUsersResponse;
import com.sentimospadel.backend.club.dto.ClubQuickActionRequest;
import com.sentimospadel.backend.club.dto.ClubQuickActionResponse;
import com.sentimospadel.backend.club.service.ClubManagementService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs/me/management")
@RequiredArgsConstructor
public class ClubManagementController {

    private final ClubManagementService clubManagementService;

    @GetMapping("/dashboard")
    public ClubManagementDashboardResponse getDashboard(Authentication authentication) {
        return clubManagementService.getDashboard(authentication.getName());
    }

    @GetMapping("/users")
    public ClubManagementUsersResponse getUsers(Authentication authentication) {
        return clubManagementService.getUsersOverview(authentication.getName());
    }

    @GetMapping("/agenda")
    public ClubManagementAgendaResponse getAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        return clubManagementService.getAgenda(authentication.getName(), date);
    }

    @PostMapping("/agenda/slot-action")
    public ClubManagementAgendaResponse applyAgendaSlotAction(
            @Valid @RequestBody ClubAgendaSlotActionRequest request,
            Authentication authentication
    ) {
        return clubManagementService.applyAgendaSlotAction(authentication.getName(), request);
    }

    @PostMapping("/quick-actions")
    public ClubQuickActionResponse executeQuickAction(
            @Valid @RequestBody ClubQuickActionRequest request,
            Authentication authentication
    ) {
        return clubManagementService.executeQuickAction(authentication.getName(), request);
    }
}
