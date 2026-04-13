package com.sentimospadel.backend.tournament.controller;

import com.sentimospadel.backend.tournament.dto.CreateTournamentRequest;
import com.sentimospadel.backend.tournament.dto.LaunchTournamentRequest;
import com.sentimospadel.backend.tournament.dto.TournamentInviteLinkResponse;
import com.sentimospadel.backend.tournament.dto.TournamentInvitePreviewResponse;
import com.sentimospadel.backend.tournament.dto.TournamentLaunchPreviewResponse;
import com.sentimospadel.backend.tournament.dto.RejectTournamentMatchResultRequest;
import com.sentimospadel.backend.tournament.dto.SubmitTournamentMatchResultRequest;
import com.sentimospadel.backend.tournament.dto.SyncTournamentEntriesRequest;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResultResponse;
import com.sentimospadel.backend.tournament.dto.TournamentResponse;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsResponse;
import com.sentimospadel.backend.tournament.dto.UpsertMyTournamentEntryRequest;
import com.sentimospadel.backend.tournament.dto.UpdateTournamentEntryTeamNameRequest;
import com.sentimospadel.backend.tournament.service.TournamentInviteService;
import com.sentimospadel.backend.tournament.service.TournamentMatchService;
import com.sentimospadel.backend.tournament.service.TournamentService;
import com.sentimospadel.backend.tournament.service.TournamentStandingsService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentInviteService tournamentInviteService;
    private final TournamentMatchService tournamentMatchService;
    private final TournamentStandingsService tournamentStandingsService;

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            Authentication authentication,
            @Valid @RequestBody CreateTournamentRequest request
    ) {
        TournamentResponse response = tournamentService.createTournament(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/tournaments/" + response.id())).body(response);
    }

    @PostMapping("/{id}/join")
    public TournamentResponse joinTournament(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) UpsertMyTournamentEntryRequest request
    ) {
        return tournamentService.joinTournament(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/leave")
    public TournamentResponse leaveTournament(Authentication authentication, @PathVariable Long id) {
        return tournamentService.leaveTournament(authentication.getName(), id);
    }

    @PutMapping("/{id}/entries")
    public TournamentResponse syncEntries(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SyncTournamentEntriesRequest request
    ) {
        return tournamentService.syncEntries(authentication.getName(), id, request);
    }

    @PutMapping("/{id}/entries/me")
    public TournamentResponse updateMyEntry(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpsertMyTournamentEntryRequest request
    ) {
        return tournamentService.updateMyEntry(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/launch")
    public TournamentResponse launchTournament(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody LaunchTournamentRequest request
    ) {
        return tournamentService.launchTournament(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/launch-preview")
    public TournamentLaunchPreviewResponse previewTournamentLaunch(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody LaunchTournamentRequest request
    ) {
        return tournamentService.previewLaunchTournament(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/invite-link")
    public TournamentInviteLinkResponse createInviteLink(Authentication authentication, @PathVariable Long id) {
        return tournamentInviteService.createInviteLink(authentication.getName(), id);
    }

    @PutMapping("/{id}/entries/me/team-name")
    public TournamentResponse updateMyEntryTeamName(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTournamentEntryTeamNameRequest request
    ) {
        return tournamentService.updateMyEntryTeamName(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/archive")
    public TournamentResponse archiveTournament(Authentication authentication, @PathVariable Long id) {
        return tournamentService.archiveTournament(authentication.getName(), id);
    }

    @GetMapping
    public List<TournamentResponse> getTournaments() {
        return tournamentService.getTournaments();
    }

    @GetMapping("/{id}")
    public TournamentResponse getTournamentById(@PathVariable Long id) {
        return tournamentService.getTournamentById(id);
    }

    @GetMapping("/{id}/matches")
    public List<TournamentMatchResponse> getTournamentMatches(@PathVariable Long id) {
        return tournamentMatchService.getTournamentMatches(id);
    }

    @PostMapping("/{id}/matches/{matchId}/result")
    public ResponseEntity<TournamentMatchResultResponse> submitMatchResult(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long matchId,
            @Valid @RequestBody SubmitTournamentMatchResultRequest request
    ) {
        return ResponseEntity.created(URI.create("/api/tournaments/" + id + "/matches/" + matchId + "/result"))
                .body(tournamentMatchService.submitResult(authentication.getName(), id, matchId, request));
    }

    @PostMapping("/{id}/matches/{matchId}/result/confirm")
    public TournamentMatchResultResponse confirmMatchResult(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long matchId
    ) {
        return tournamentMatchService.confirmResult(authentication.getName(), id, matchId);
    }

    @PostMapping("/{id}/matches/{matchId}/result/reject")
    public TournamentMatchResultResponse rejectMatchResult(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long matchId,
            @RequestBody(required = false) RejectTournamentMatchResultRequest request
    ) {
        return tournamentMatchService.rejectResult(authentication.getName(), id, matchId, request);
    }

    @GetMapping("/{id}/standings")
    public TournamentStandingsResponse getStandings(@PathVariable Long id) {
        return tournamentStandingsService.getStandings(id);
    }

    @GetMapping("/invite")
    public TournamentInvitePreviewResponse resolveInvite(@RequestParam String token) {
        return tournamentInviteService.resolveInvite(token);
    }
}
