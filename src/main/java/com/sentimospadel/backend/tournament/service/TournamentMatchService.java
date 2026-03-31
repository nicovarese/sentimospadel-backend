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
import com.sentimospadel.backend.tournament.dto.TournamentStandingsEntryResponse;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsGroupResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final TournamentStandingsService tournamentStandingsService;

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

        if (match.getTournament().getFormat() == TournamentFormat.ELIMINATION) {
            advanceEliminationBracketIfNeeded(match.getTournament());
        }
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

    private void advanceEliminationBracketIfNeeded(Tournament tournament) {
        List<TournamentMatch> matches = tournamentMatchRepository.findAllByTournamentIdOrderByRoundNumberAscIdAsc(tournament.getId());

        List<TournamentMatch> quarterfinals = matches.stream()
                .filter(match -> match.getPhase() == TournamentMatchPhase.QUARTERFINAL)
                .toList();
        List<TournamentMatch> semifinals = matches.stream()
                .filter(match -> match.getPhase() == TournamentMatchPhase.SEMIFINAL)
                .toList();
        List<TournamentMatch> finals = matches.stream()
                .filter(match -> match.getPhase() == TournamentMatchPhase.FINAL)
                .toList();
        List<TournamentMatch> groupMatches = matches.stream()
                .filter(match -> match.getPhase() == TournamentMatchPhase.GROUP_STAGE)
                .toList();

        if (!groupMatches.isEmpty()
                && allCompleted(groupMatches)
                && quarterfinals.isEmpty()
                && semifinals.isEmpty()
                && finals.isEmpty()) {
            tournamentMatchRepository.saveAll(generateInitialEliminationPlayoffs(tournament, matches));
            return;
        }

        if (!quarterfinals.isEmpty() && allCompleted(quarterfinals) && semifinals.isEmpty()) {
            tournamentMatchRepository.saveAll(generateSemifinalsFromCompletedPhase(tournament, quarterfinals, matches));
            return;
        }

        if (!semifinals.isEmpty() && allCompleted(semifinals) && finals.isEmpty()) {
            tournamentMatchRepository.save(buildFinalFromCompletedPhase(tournament, semifinals, matches));
        }
    }

    private List<TournamentMatch> generateInitialEliminationPlayoffs(Tournament tournament, List<TournamentMatch> existingMatches) {
        List<TournamentStandingsGroupResponse> groups = tournamentStandingsService.buildEliminationGroups(tournament);
        if (groups.isEmpty()) {
            return List.of();
        }

        if (groups.size() == 1) {
            List<TournamentEntry> qualified = topEntries(tournament, groups.getFirst().standings(), 4);
            if (qualified.size() >= 4) {
                return buildQuarterOrSemifinalRound(
                        tournament,
                        TournamentMatchPhase.SEMIFINAL,
                        List.of(
                                qualified.get(0), qualified.get(3),
                                qualified.get(1), qualified.get(2)
                        ),
                        existingMatches
                );
            }
            if (qualified.size() == 2) {
                return List.of(buildSingleKnockoutMatch(tournament, TournamentMatchPhase.FINAL, 1, "Final", qualified.get(0), qualified.get(1), existingMatches, 0));
            }
            throw new ConflictException("ELIMINATION tournaments need at least two qualified teams to build playoffs");
        }

        if (groups.size() == 2) {
            List<TournamentEntry> groupA = topEntries(tournament, groups.get(0).standings(), 2);
            List<TournamentEntry> groupB = topEntries(tournament, groups.get(1).standings(), 2);
            if (groupA.size() >= 2 && groupB.size() >= 2) {
                return buildQuarterOrSemifinalRound(
                        tournament,
                        TournamentMatchPhase.SEMIFINAL,
                        List.of(
                                groupA.get(0), groupB.get(1),
                                groupB.get(0), groupA.get(1)
                        ),
                        existingMatches
                );
            }
            if (!groupA.isEmpty() && !groupB.isEmpty()) {
                return List.of(buildSingleKnockoutMatch(tournament, TournamentMatchPhase.FINAL, 1, "Final", groupA.get(0), groupB.get(0), existingMatches, 0));
            }
            throw new ConflictException("Both groups need qualified teams before playoff generation");
        }

        List<TournamentEntry> winners = groups.stream()
                .map(TournamentStandingsGroupResponse::standings)
                .filter(groupStandings -> !groupStandings.isEmpty())
                .map(groupStandings -> groupStandings.getFirst())
                .sorted(Comparator.comparingInt(TournamentStandingsEntryResponse::points).reversed()
                        .thenComparingInt(TournamentStandingsEntryResponse::gamesDifference).reversed()
                        .thenComparingInt(TournamentStandingsEntryResponse::gamesWon).reversed())
                .map(entry -> resolveEntry(tournament, entry.tournamentEntryId()))
                .toList();

        if (winners.size() == 2) {
            return List.of(buildSingleKnockoutMatch(tournament, TournamentMatchPhase.FINAL, 1, "Final", winners.get(0), winners.get(1), existingMatches, 0));
        }
        if (winners.size() == 4) {
            return buildQuarterOrSemifinalRound(
                    tournament,
                    TournamentMatchPhase.SEMIFINAL,
                    List.of(
                            winners.get(0), winners.get(3),
                            winners.get(1), winners.get(2)
                    ),
                    existingMatches
            );
        }
        if (winners.size() == 8) {
            return buildQuarterOrSemifinalRound(
                    tournament,
                    TournamentMatchPhase.QUARTERFINAL,
                    List.of(
                            winners.get(0), winners.get(7),
                            winners.get(3), winners.get(4),
                            winners.get(1), winners.get(6),
                            winners.get(2), winners.get(5)
                    ),
                    existingMatches
            );
        }

        throw new ConflictException("ELIMINATION playoff generation currently supports 2, 4 or 8 qualified teams");
    }

    private List<TournamentMatch> generateSemifinalsFromCompletedPhase(
            Tournament tournament,
            List<TournamentMatch> completedQuarterfinals,
            List<TournamentMatch> existingMatches
    ) {
        List<TournamentEntry> winners = completedQuarterfinals.stream()
                .sorted(Comparator.comparing(TournamentMatch::getRoundNumber))
                .map(this::winnerEntry)
                .toList();

        return buildQuarterOrSemifinalRound(
                tournament,
                TournamentMatchPhase.SEMIFINAL,
                List.of(
                        winners.get(0), winners.get(1),
                        winners.get(2), winners.get(3)
                ),
                existingMatches
        );
    }

    private TournamentMatch buildFinalFromCompletedPhase(
            Tournament tournament,
            List<TournamentMatch> completedSemifinals,
            List<TournamentMatch> existingMatches
    ) {
        List<TournamentEntry> winners = completedSemifinals.stream()
                .sorted(Comparator.comparing(TournamentMatch::getRoundNumber))
                .map(this::winnerEntry)
                .toList();

        return buildSingleKnockoutMatch(
                tournament,
                TournamentMatchPhase.FINAL,
                1,
                "Final",
                winners.get(0),
                winners.get(1),
                existingMatches,
                0
        );
    }

    private List<TournamentMatch> buildQuarterOrSemifinalRound(
            Tournament tournament,
            TournamentMatchPhase phase,
            List<TournamentEntry> seededEntries,
            List<TournamentMatch> existingMatches
    ) {
        List<TournamentMatch> matches = new ArrayList<>();
        int matchCount = seededEntries.size() / 2;
        String roundLabelPrefix = phase == TournamentMatchPhase.QUARTERFINAL ? "Cuartos de Final " : "Semifinal ";
        for (int index = 0; index < matchCount; index++) {
            matches.add(buildSingleKnockoutMatch(
                    tournament,
                    phase,
                    index + 1,
                    roundLabelPrefix + (index + 1),
                    seededEntries.get(index * 2),
                    seededEntries.get(index * 2 + 1),
                    existingMatches,
                    index
            ));
        }
        return matches;
    }

    private TournamentMatch buildSingleKnockoutMatch(
            Tournament tournament,
            TournamentMatchPhase phase,
            int roundNumber,
            String roundLabel,
            TournamentEntry teamOne,
            TournamentEntry teamTwo,
            List<TournamentMatch> existingMatches,
            int slotOffset
    ) {
        List<String> courtNames = tournament.getCourtNames() == null || tournament.getCourtNames().isEmpty()
                ? List.of("Cancha 1")
                : tournament.getCourtNames();
        int availableCourts = tournament.getAvailableCourts() == null || tournament.getAvailableCourts() <= 0
                ? 1
                : tournament.getAvailableCourts();

        Instant lastScheduledAt = existingMatches.stream()
                .map(TournamentMatch::getScheduledAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(tournament.getStartDate().atTime(18, 0).toInstant(ZoneOffset.UTC));
        int roundBase = existingMatches.stream()
                .map(TournamentMatch::getRoundNumber)
                .max(Integer::compareTo)
                .orElse(0);

        LocalDate baseDate = lastScheduledAt.atZone(ZoneOffset.UTC).toLocalDate().plusDays(1);
        LocalTime baseTime = switch (phase) {
            case FINAL -> LocalTime.of(19, 0);
            case SEMIFINAL -> LocalTime.of(18 + (slotOffset * 2), 0);
            case QUARTERFINAL -> LocalTime.of(16 + (slotOffset * 2), 0);
            default -> LocalTime.of(18, 0);
        };

        Instant scheduledAt = baseDate.atTime(baseTime).toInstant(ZoneOffset.UTC);

        return TournamentMatch.builder()
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .phase(phase)
                .status(TournamentMatchStatus.SCHEDULED)
                .roundNumber(roundBase + roundNumber)
                .legNumber(null)
                .roundLabel(roundLabel)
                .scheduledAt(scheduledAt)
                .courtName(courtNames.get(Math.min(slotOffset % availableCourts, courtNames.size() - 1)))
                .build();
    }

    private TournamentEntry resolveEntry(Tournament tournament, Long entryId) {
        return tournamentService.getEntries(tournament.getId()).stream()
                .filter(entry -> entry.getId().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new ConflictException("Tournament entry " + entryId + " was not found during playoff generation"));
    }

    private List<TournamentEntry> topEntries(Tournament tournament, List<TournamentStandingsEntryResponse> standings, int maxEntries) {
        return standings.stream()
                .limit(maxEntries)
                .map(entry -> resolveEntry(tournament, entry.tournamentEntryId()))
                .toList();
    }

    private TournamentEntry winnerEntry(TournamentMatch match) {
        TournamentMatchResult result = tournamentMatchResultRepository.findByTournamentMatchId(match.getId())
                .orElseThrow(() -> new ConflictException("A completed tournament match must have a stored result"));
        return result.getWinnerTeam() == MatchWinnerTeam.TEAM_ONE
                ? match.getTeamOneEntry()
                : match.getTeamTwoEntry();
    }

    private boolean allCompleted(List<TournamentMatch> matches) {
        return !matches.isEmpty() && matches.stream().allMatch(match -> match.getStatus() == TournamentMatchStatus.COMPLETED);
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
        List<TournamentMatch> matches = tournamentMatchRepository.findAllByTournamentIdOrderByRoundNumberAscIdAsc(tournament.getId());
        boolean allCompleted = !matches.isEmpty() && matches.stream().allMatch(match -> match.getStatus() == TournamentMatchStatus.COMPLETED);

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
