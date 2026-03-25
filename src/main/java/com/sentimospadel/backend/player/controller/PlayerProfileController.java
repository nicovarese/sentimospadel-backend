package com.sentimospadel.backend.player.controller;

import com.sentimospadel.backend.player.dto.PlayerProfileResponse;
import com.sentimospadel.backend.player.service.PlayerProfileService;
import com.sentimospadel.backend.match.dto.PlayerMatchHistoryEntryResponse;
import com.sentimospadel.backend.match.enums.PlayerMatchHistoryScope;
import com.sentimospadel.backend.match.service.PlayerMatchHistoryService;
import com.sentimospadel.backend.rating.dto.RatingHistoryEntryResponse;
import com.sentimospadel.backend.rating.service.PlayerRatingHistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerProfileController {

    private final PlayerProfileService playerProfileService;
    private final PlayerRatingHistoryService playerRatingHistoryService;
    private final PlayerMatchHistoryService playerMatchHistoryService;

    @GetMapping("/me")
    public PlayerProfileResponse getMyPlayerProfile(Authentication authentication) {
        return playerProfileService.getMyPlayerProfile(authentication.getName());
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
