package com.sentimospadel.backend.club.controller;

import com.sentimospadel.backend.club.dto.ClubResponse;
import com.sentimospadel.backend.club.dto.CreateClubRequest;
import com.sentimospadel.backend.club.service.ClubService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping
    public ResponseEntity<ClubResponse> createClub(@Valid @RequestBody CreateClubRequest request) {
        ClubResponse response = clubService.createClub(request);
        return ResponseEntity.created(URI.create("/api/clubs/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ClubResponse getClubById(@PathVariable Long id) {
        return clubService.getClubById(id);
    }

    @GetMapping
    public List<ClubResponse> getClubs() {
        return clubService.getClubs();
    }
}
