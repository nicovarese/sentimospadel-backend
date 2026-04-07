package com.sentimospadel.backend.club.controller;

import com.sentimospadel.backend.club.dto.ClubAgendaSlotActionRequest;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubManagementCourtsResponse;
import com.sentimospadel.backend.club.dto.ClubManagementDashboardResponse;
import com.sentimospadel.backend.club.dto.ClubManagementUsersResponse;
import com.sentimospadel.backend.club.dto.ClubQuickActionRequest;
import com.sentimospadel.backend.club.dto.ClubQuickActionResponse;
import com.sentimospadel.backend.club.dto.CreateClubCourtRequest;
import com.sentimospadel.backend.club.dto.ReorderClubCourtsRequest;
import com.sentimospadel.backend.club.dto.UpdateClubCourtRequest;
import com.sentimospadel.backend.club.service.ClubManagementService;
import com.sentimospadel.backend.verification.dto.ClubVerificationDecisionRequest;
import com.sentimospadel.backend.verification.dto.ClubVerificationManagementRequestResponse;
import com.sentimospadel.backend.verification.service.ClubVerificationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs/me/management")
@RequiredArgsConstructor
public class ClubManagementController {

    private final ClubManagementService clubManagementService;
    private final ClubVerificationService clubVerificationService;

    @GetMapping("/dashboard")
    public ClubManagementDashboardResponse getDashboard(Authentication authentication) {
        return clubManagementService.getDashboard(authentication.getName());
    }

    @GetMapping("/users")
    public ClubManagementUsersResponse getUsers(Authentication authentication) {
        return clubManagementService.getUsersOverview(authentication.getName());
    }

    @GetMapping("/courts")
    public ClubManagementCourtsResponse getCourts(Authentication authentication) {
        return clubManagementService.getCourts(authentication.getName());
    }

    @PostMapping("/courts")
    public ClubManagementCourtsResponse createCourt(
            @Valid @RequestBody CreateClubCourtRequest request,
            Authentication authentication
    ) {
        return clubManagementService.createCourt(authentication.getName(), request);
    }

    @PutMapping("/courts/{courtId}")
    public ClubManagementCourtsResponse updateCourt(
            @PathVariable Long courtId,
            @Valid @RequestBody UpdateClubCourtRequest request,
            Authentication authentication
    ) {
        return clubManagementService.updateCourt(authentication.getName(), courtId, request);
    }

    @PostMapping("/courts/reorder")
    public ClubManagementCourtsResponse reorderCourts(
            @Valid @RequestBody ReorderClubCourtsRequest request,
            Authentication authentication
    ) {
        return clubManagementService.reorderCourts(authentication.getName(), request);
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

    @GetMapping("/verification-requests")
    public List<ClubVerificationManagementRequestResponse> getVerificationRequests(Authentication authentication) {
        return clubVerificationService.getManagedClubRequests(authentication.getName());
    }

    @PostMapping("/verification-requests/{requestId}/approve")
    public ClubVerificationManagementRequestResponse approveVerificationRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ClubVerificationDecisionRequest request,
            Authentication authentication
    ) {
        return clubVerificationService.approveManagedClubRequest(authentication.getName(), requestId, request);
    }

    @PostMapping("/verification-requests/{requestId}/reject")
    public ClubVerificationManagementRequestResponse rejectVerificationRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ClubVerificationDecisionRequest request,
            Authentication authentication
    ) {
        return clubVerificationService.rejectManagedClubRequest(authentication.getName(), requestId, request);
    }
}
