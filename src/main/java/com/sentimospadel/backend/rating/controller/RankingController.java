package com.sentimospadel.backend.rating.controller;

import com.sentimospadel.backend.rating.dto.RankingEntryResponse;
import com.sentimospadel.backend.rating.service.RankingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public List<RankingEntryResponse> getRankings() {
        return rankingService.getRankings();
    }
}
