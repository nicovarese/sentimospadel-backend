package com.sentimospadel.backend.player.controller;

import com.sentimospadel.backend.player.dto.PlayerProfileResponse;
import com.sentimospadel.backend.player.dto.PlayerClubRankingSummaryResponse;
import com.sentimospadel.backend.player.dto.PlayerPartnerInsightResponse;
import com.sentimospadel.backend.player.dto.PlayerRivalInsightResponse;
import com.sentimospadel.backend.player.dto.UpdatePlayerProfileRequest;
import com.sentimospadel.backend.player.service.PlayerProfileService;
import com.sentimospadel.backend.player.service.PlayerInsightService;
import com.sentimospadel.backend.match.dto.PlayerMatchHistoryEntryResponse;
import com.sentimospadel.backend.match.enums.PlayerMatchHistoryScope;
import com.sentimospadel.backend.match.service.PlayerMatchHistoryService;
import com.sentimospadel.backend.notification.dto.PendingActionResponse;
import com.sentimospadel.backend.notification.service.PlayerInboxService;
import com.sentimospadel.backend.rating.dto.RatingHistoryEntryResponse;
import com.sentimospadel.backend.rating.service.PlayerRatingHistoryService;
import com.sentimospadel.backend.verification.dto.CreateClubVerificationRequest;
import com.sentimospadel.backend.verification.dto.PlayerClubVerificationSummaryResponse;
import com.sentimospadel.backend.verification.service.ClubVerificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerProfileController {

    private final PlayerProfileService playerProfileService;
    private final PlayerInsightService playerInsightService;
    private final PlayerRatingHistoryService playerRatingHistoryService;
    private final PlayerMatchHistoryService playerMatchHistoryService;
    private final PlayerInboxService playerInboxService;
    private final ClubVerificationService clubVerificationService;

    @GetMapping("/me")
    public PlayerProfileResponse getMyPlayerProfile(Authentication authentication) {
        return playerProfileService.getMyPlayerProfile(authentication.getName());
    }

    @PutMapping("/me")
    public PlayerProfileResponse updateMyPlayerProfile(
            Authentication authentication,
            @Valid @RequestBody UpdatePlayerProfileRequest request
    ) {
        return playerProfileService.updateMyPlayerProfile(authentication.getName(), request);
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlayerProfileResponse uploadMyPlayerPhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        return playerProfileService.updateMyPlayerPhoto(authentication.getName(), file);
    }

    @GetMapping("/me/rating-history")
    public List<RatingHistoryEntryResponse> getMyRatingHistory(Authentication authentication) {
        return playerRatingHistoryService.getMyRatingHistory(authentication.getName());
    }

    @GetMapping("/me/matches")
    public List<PlayerMatchHistoryEntryResponse> getMyMatches(
            Authentication authentication,
            @RequestParam(required = false) String scope
    ) {
        return playerMatchHistoryService.getMyMatches(
                authentication.getName(),
                PlayerMatchHistoryScope.fromQuery(scope)
        );
    }

    @GetMapping("/me/pending-actions")
    public List<PendingActionResponse> getMyPendingActions(Authentication authentication) {
        return playerInboxService.getMyPendingActions(authentication.getName());
    }

    @GetMapping("/me/club-verification")
    public PlayerClubVerificationSummaryResponse getMyClubVerification(Authentication authentication) {
        return clubVerificationService.getMyVerificationSummary(authentication.getName());
    }

    @PostMapping("/me/club-verification/request")
    public PlayerClubVerificationSummaryResponse createMyClubVerificationRequest(
            Authentication authentication,
            @Valid @RequestBody CreateClubVerificationRequest request
    ) {
        return clubVerificationService.createMyVerificationRequest(authentication.getName(), request);
    }

    @GetMapping("/me/top-partners")
    public List<PlayerPartnerInsightResponse> getMyTopPartners(Authentication authentication) {
        return playerInsightService.getMyTopPartners(authentication.getName());
    }

    @GetMapping("/me/top-rivals")
    public List<PlayerRivalInsightResponse> getMyTopRivals(Authentication authentication) {
        return playerInsightService.getMyTopRivals(authentication.getName());
    }

    @GetMapping("/me/club-rankings")
    public List<PlayerClubRankingSummaryResponse> getMyClubRankings(Authentication authentication) {
        return playerInsightService.getMyClubRankings(authentication.getName());
    }

    @GetMapping("/{id}")
    public PlayerProfileResponse getPlayerProfileById(@PathVariable Long id) {
        return playerProfileService.getPlayerProfileById(id);
    }

    @GetMapping("/{id}/rating-history")
    public List<RatingHistoryEntryResponse> getRatingHistoryByPlayerId(@PathVariable Long id) {
        return playerRatingHistoryService.getRatingHistoryForPlayer(id);
    }

    @GetMapping
    public List<PlayerProfileResponse> getPlayerProfiles() {
        return playerProfileService.getPlayerProfiles();
    }
}
