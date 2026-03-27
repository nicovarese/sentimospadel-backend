package com.sentimospadel.backend.tournament.service;

import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.shared.result.ResultEligibilityPolicy;
import com.sentimospadel.backend.tournament.dto.RejectTournamentMatchResultRequest;
import com.sentimospadel.backend.tournament.dto.SubmitTournamentMatchResultRequest;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResultResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchScoreSetRequest;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentMatchService {

    private final TournamentService tournamentService;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentMatchResultRepository tournamentMatchResultRepository;
    private final PlayerProfileResolverService playerProfileResolverService;
    private final TournamentMapper tournamentMapper;

    @Transactional(readOnly = true)
    public List<TournamentMatchResponse> getTournamentMatches(Long tournamentId) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        List<TournamentMatchResult> results = tournamentMatchResultRepository.findAllByTournamentMatchTournamentId(tournamentId);
        return toMatchResponses(tournamentMatchRepository.findAllByTournamentIdOrderByRoundNumberAscIdAsc(tournament.getId()), results);
    }

    @Transactional
    public TournamentMatchResultResponse submitResult(
            String email,
            Long tournamentId,
            Long matchId,
            SubmitTournamentMatchResultRequest request
    ) {
        TournamentMatch match = getMatchEntity(tournamentId, matchId);
        Tournament tournament = match.getTournament();
        PlayerProfile submitter = playerProfileResolverService.getOrCreateByUserEmail(email);
        Instant now = Instant.now();

        if (tournament.getFormat() != TournamentFormat.LEAGUE) {
            throw new ConflictException("Tournament match results are currently operational only for LEAGUE tournaments");
        }
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS && tournament.getStatus() != TournamentStatus.COMPLETED) {
            throw new ConflictException("Results can only be submitted after tournament launch");
        }
        if (!isPlayerInMatch(match, submitter.getId())) {
            throw new AccessDeniedException("Only team members can submit tournament results");
        }
        if (!ResultEligibilityPolicy.hasEnded(match.getScheduledAt(), now)) {
            throw new ConflictException("Tournament results can only be submitted once the scheduled match has ended");
        }
        if (match.getTeamOneEntry().getStatus() != TournamentEntryStatus.CONFIRMED
                || match.getTeamTwoEntry().getStatus() != TournamentEntryStatus.CONFIRMED) {
            throw new ConflictException("Tournament matches require confirmed teams");
        }
        validateSubmittedResult(request);

        TournamentMatchResult savedResult = tournamentMatchResultRepository.save(
                tournamentMatchResultRepository.findByTournamentMatchId(matchId)
                        .map(existing -> prepareResubmittedResult(existing, submitter, request))
                        .orElseGet(() -> buildNewResult(match, submitter, request, now))
        );

        match.setStatus(TournamentMatchStatus.RESULT_PENDING);
        tournamentMatchRepository.save(match);
        return tournamentMapper.toMatchResultResponse(savedResult);
    }

    @Transactional
    public TournamentMatchResultResponse confirmResult(String email, Long tournamentId, Long matchId) {
        TournamentMatch match = getMatchEntity(tournamentId, matchId);
        PlayerProfile confirmer = playerProfileResolverService.getOrCreateByUserEmail(email);
        TournamentMatchResult result = getResultEntity(matchId);

        if (result.getStatus() == TournamentMatchResultStatus.CONFIRMED) {
            return tournamentMapper.toMatchResultResponse(result);
        }
        if (result.getStatus() == TournamentMatchResultStatus.REJECTED) {
            throw new ConflictException("Rejected tournament results must be resubmitted before confirmation");
        }

        ensureOppositeTeamAction(match, result.getSubmittedBy().getId(), confirmer.getId(), "confirm");

        result.setStatus(TournamentMatchResultStatus.CONFIRMED);
        result.setConfirmedBy(confirmer);
        result.setConfirmedAt(Instant.now());
        TournamentMatchResult savedResult = tournamentMatchResultRepository.save(result);

        match.setStatus(TournamentMatchStatus.COMPLETED);
        tournamentMatchRepository.save(match);
        updateTournamentCompletionIfNeeded(match.getTournament());

        return tournamentMapper.toMatchResultResponse(savedResult);
    }

    @Transactional
    public TournamentMatchResultResponse rejectResult(
            String email,
            Long tournamentId,
            Long matchId,
            RejectTournamentMatchResultRequest request
    ) {
        TournamentMatch match = getMatchEntity(tournamentId, matchId);
        PlayerProfile rejector = playerProfileResolverService.getOrCreateByUserEmail(email);
        TournamentMatchResult result = getResultEntity(matchId);

        if (result.getStatus() == TournamentMatchResultStatus.CONFIRMED) {
            throw new ConflictException("Confirmed tournament results cannot be rejected");
        }
        if (result.getStatus() == TournamentMatchResultStatus.REJECTED) {
            return tournamentMapper.toMatchResultResponse(result);
        }

        ensureOppositeTeamAction(match, result.getSubmittedBy().getId(), rejector.getId(), "reject");

        result.setStatus(TournamentMatchResultStatus.REJECTED);
        result.setRejectedBy(rejector);
        result.setRejectedAt(Instant.now());
        result.setRejectionReason(trimToNull(request == null ? null : request.rejectionReason()));
        TournamentMatchResult savedResult = tournamentMatchResultRepository.save(result);

        match.setStatus(TournamentMatchStatus.SCHEDULED);
        tournamentMatchRepository.save(match);
        return tournamentMapper.toMatchResultResponse(savedResult);
    }

    private TournamentMatchResult buildNewResult(
            TournamentMatch match,
            PlayerProfile submitter,
            SubmitTournamentMatchResultRequest request,
            Instant submittedAt
    ) {
        return TournamentMatchResult.builder()
                .tournamentMatch(match)
                .submittedBy(submitter)
                .status(TournamentMatchResultStatus.PENDING)
                .winnerTeam(request.winnerTeam())
                .setOneTeamOneGames(setValue(request.sets(), 0, true))
                .setOneTeamTwoGames(setValue(request.sets(), 0, false))
                .setTwoTeamOneGames(setValue(request.sets(), 1, true))
                .setTwoTeamTwoGames(setValue(request.sets(), 1, false))
                .setThreeTeamOneGames(setValue(request.sets(), 2, true))
                .setThreeTeamTwoGames(setValue(request.sets(), 2, false))
                .submittedAt(submittedAt)
                .build();
    }

    private TournamentMatchResult prepareResubmittedResult(
            TournamentMatchResult existingResult,
            PlayerProfile submitter,
            SubmitTournamentMatchResultRequest request
    ) {
        if (existingResult.getStatus() == TournamentMatchResultStatus.CONFIRMED
                || existingResult.getStatus() == TournamentMatchResultStatus.PENDING) {
            throw new ConflictException("This tournament match already has a submitted result");
        }

        existingResult.setSubmittedBy(submitter);
        existingResult.setStatus(TournamentMatchResultStatus.PENDING);
        existingResult.setWinnerTeam(request.winnerTeam());
        existingResult.setSetOneTeamOneGames(setValue(request.sets(), 0, true));
        existingResult.setSetOneTeamTwoGames(setValue(request.sets(), 0, false));
        existingResult.setSetTwoTeamOneGames(setValue(request.sets(), 1, true));
        existingResult.setSetTwoTeamTwoGames(setValue(request.sets(), 1, false));
        existingResult.setSetThreeTeamOneGames(setValue(request.sets(), 2, true));
        existingResult.setSetThreeTeamTwoGames(setValue(request.sets(), 2, false));
        existingResult.setSubmittedAt(Instant.now());
        existingResult.setConfirmedBy(null);
        existingResult.setConfirmedAt(null);
        existingResult.setRejectedBy(null);
        existingResult.setRejectedAt(null);
        existingResult.setRejectionReason(null);
        return existingResult;
    }

    private void validateSubmittedResult(SubmitTournamentMatchResultRequest request) {
        if (request.sets().isEmpty()) {
            throw new BadRequestException("Tournament result must include at least one set");
        }

        int setsWonByTeamOne = 0;
        int setsWonByTeamTwo = 0;

        for (TournamentMatchScoreSetRequest set : request.sets()) {
            if (set.teamOneGames().equals(set.teamTwoGames())) {
                throw new BadRequestException("Tournament set scores cannot be tied");
            }
            if (set.teamOneGames() > set.teamTwoGames()) {
                setsWonByTeamOne++;
            } else {
                setsWonByTeamTwo++;
            }
        }

        if (setsWonByTeamOne == setsWonByTeamTwo) {
            throw new BadRequestException("Tournament match result cannot end tied in sets");
        }

        if (request.winnerTeam() == MatchWinnerTeam.TEAM_ONE && setsWonByTeamOne <= setsWonByTeamTwo) {
            throw new BadRequestException("Winner team must also win more sets");
        }
        if (request.winnerTeam() == MatchWinnerTeam.TEAM_TWO && setsWonByTeamTwo <= setsWonByTeamOne) {
            throw new BadRequestException("Winner team must also win more sets");
        }
    }

    private void ensureOppositeTeamAction(
            TournamentMatch match,
            Long submittedByPlayerId,
            Long actorPlayerId,
            String action
    ) {
        if (submittedByPlayerId.equals(actorPlayerId)) {
            throw new AccessDeniedException("The submitting player cannot " + action + " their own result");
        }

        TournamentEntry submittedByTeam = teamForPlayer(match, submittedByPlayerId);
        TournamentEntry actorTeam = teamForPlayer(match, actorPlayerId);
        if (actorTeam == null) {
            throw new AccessDeniedException("Only team members can " + action + " tournament results");
        }
        if (submittedByTeam == null) {
            throw new ConflictException("Tournament teams must be assigned before result " + action);
        }
        if (submittedByTeam.getId().equals(actorTeam.getId())) {
            throw new AccessDeniedException("Tournament result " + action + " must come from the opposite team");
        }
    }

    private TournamentEntry teamForPlayer(TournamentMatch match, Long playerProfileId) {
        if (isPlayerInEntry(match.getTeamOneEntry(), playerProfileId)) {
            return match.getTeamOneEntry();
        }
        if (isPlayerInEntry(match.getTeamTwoEntry(), playerProfileId)) {
            return match.getTeamTwoEntry();
        }
        return null;
    }

    private boolean isPlayerInMatch(TournamentMatch match, Long playerProfileId) {
        return teamForPlayer(match, playerProfileId) != null;
    }

    private boolean isPlayerInEntry(TournamentEntry entry, Long playerProfileId) {
        return entry.getPrimaryPlayerProfile() != null && entry.getPrimaryPlayerProfile().getId().equals(playerProfileId)
                || entry.getSecondaryPlayerProfile() != null && entry.getSecondaryPlayerProfile().getId().equals(playerProfileId);
    }

    private Integer setValue(List<TournamentMatchScoreSetRequest> sets, int index, boolean teamOne) {
        if (index >= sets.size()) {
            return null;
        }
        return teamOne ? sets.get(index).teamOneGames() : sets.get(index).teamTwoGames();
    }

    private void updateTournamentCompletionIfNeeded(Tournament tournament) {
        boolean allCompleted = tournamentMatchRepository.findAllByTournamentIdOrderByRoundNumberAscIdAsc(tournament.getId())
                .stream()
                .allMatch(match -> match.getStatus() == TournamentMatchStatus.COMPLETED);

        if (allCompleted) {
            tournament.setStatus(TournamentStatus.COMPLETED);
        } else if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            tournament.setStatus(TournamentStatus.IN_PROGRESS);
        }
    }

    private TournamentMatch getMatchEntity(Long tournamentId, Long matchId) {
        tournamentService.getTournamentEntity(tournamentId);
        return tournamentMatchRepository.findByIdAndTournamentId(matchId, tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tournament match with id " + matchId + " was not found for tournament " + tournamentId
                ));
    }

    private TournamentMatchResult getResultEntity(Long matchId) {
        return tournamentMatchResultRepository.findByTournamentMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Result for tournament match with id " + matchId + " was not found"
                ));
    }

    private List<TournamentMatchResponse> toMatchResponses(
            List<TournamentMatch> matches,
            List<TournamentMatchResult> results
    ) {
        return matches.stream()
                .map(match -> tournamentMapper.toMatchResponse(
                        match,
                        results.stream()
                                .filter(result -> result.getTournamentMatch().getId().equals(match.getId()))
                                .findFirst()
                                .orElse(null)
                ))
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
