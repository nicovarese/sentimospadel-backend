package com.sentimospadel.backend.coach.controller;

import com.sentimospadel.backend.coach.dto.CoachResponse;
import com.sentimospadel.backend.coach.service.CoachService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coaches")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @GetMapping
    public List<CoachResponse> getCoaches() {
        return coachService.getCoaches();
    }
}
