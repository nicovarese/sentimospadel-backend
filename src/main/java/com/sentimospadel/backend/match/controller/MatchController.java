package com.sentimospadel.backend.match.controller;

import com.sentimospadel.backend.match.dto.AssignMatchTeamsRequest;
import com.sentimospadel.backend.match.dto.CreateMatchRequest;
import com.sentimospadel.backend.match.dto.MatchResultResponse;
import com.sentimospadel.backend.match.dto.MatchResponse;
import com.sentimospadel.backend.match.dto.RejectMatchResultRequest;
import com.sentimospadel.backend.match.dto.SubmitMatchResultRequest;
import com.sentimospadel.backend.match.service.MatchService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(
            Authentication authentication,
            @Valid @RequestBody CreateMatchRequest request
    ) {
        MatchResponse response = matchService.createMatch(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/matches/" + response.id())).body(response);
    }

    @PostMapping("/{id}/join")
    public MatchResponse joinMatch(Authentication authentication, @PathVariable Long id) {
        return matchService.joinMatch(authentication.getName(), id);
    }

    @PostMapping("/{id}/leave")
    public MatchResponse leaveMatch(Authentication authentication, @PathVariable Long id) {
        return matchService.leaveMatch(authentication.getName(), id);
    }

    @PostMapping("/{id}/teams")
    public MatchResponse assignTeams(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AssignMatchTeamsRequest request
    ) {
        return matchService.assignTeams(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/cancel")
    public MatchResponse cancelMatch(Authentication authentication, @PathVariable Long id) {
        return matchService.cancelMatch(authentication.getName(), id);
    }

    @PostMapping("/{id}/result")
    public ResponseEntity<MatchResultResponse> submitResult(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SubmitMatchResultRequest request
    ) {
        MatchResultResponse response = matchService.submitResult(authentication.getName(), id, request);
        return ResponseEntity.created(URI.create("/api/matches/" + id + "/result")).body(response);
    }

    @PostMapping("/{id}/result/confirm")
    public MatchResultResponse confirmResult(Authentication authentication, @PathVariable Long id) {
        return matchService.confirmResult(authentication.getName(), id);
    }

    @PostMapping("/{id}/result/reject")
    public MatchResultResponse rejectResult(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RejectMatchResultRequest request
    ) {
        return matchService.rejectResult(
                authentication.getName(),
                id,
                request == null ? new RejectMatchResultRequest(null) : request
        );
    }

    @GetMapping
    public List<MatchResponse> getMatches() {
        return matchService.getMatches();
    }

    @GetMapping("/{id}")
    public MatchResponse getMatchById(@PathVariable Long id) {
        return matchService.getMatchById(id);
    }

    @GetMapping("/{id}/result")
    public MatchResultResponse getMatchResult(@PathVariable Long id) {
        return matchService.getMatchResult(id);
    }
}
