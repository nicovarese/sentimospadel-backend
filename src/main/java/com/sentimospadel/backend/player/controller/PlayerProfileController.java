package com.sentimospadel.backend.player.controller;

import com.sentimospadel.backend.player.dto.PlayerProfileResponse;
import com.sentimospadel.backend.player.service.PlayerProfileService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerProfileController {

    private final PlayerProfileService playerProfileService;

    @GetMapping("/{id}")
    public PlayerProfileResponse getPlayerProfileById(@PathVariable Long id) {
        return playerProfileService.getPlayerProfileById(id);
    }

    @GetMapping
    public List<PlayerProfileResponse> getPlayerProfiles() {
        return playerProfileService.getPlayerProfiles();
    }
}
