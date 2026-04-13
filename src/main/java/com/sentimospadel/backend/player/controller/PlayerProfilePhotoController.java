package com.sentimospadel.backend.player.controller;

import com.sentimospadel.backend.player.service.PlayerProfilePhotoStorageService;
import com.sentimospadel.backend.player.service.StoredPlayerProfilePhotoResource;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player-profile-photos")
@RequiredArgsConstructor
public class PlayerProfilePhotoController {

    private final PlayerProfilePhotoStorageService playerProfilePhotoStorageService;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getPlayerProfilePhoto(@PathVariable String filename) {
        StoredPlayerProfilePhotoResource storedPhoto = playerProfilePhotoStorageService.load(filename);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .contentType(storedPhoto.mediaType())
                .body(storedPhoto.resource());
    }
}
