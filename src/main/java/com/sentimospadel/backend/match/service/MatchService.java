package com.sentimospadel.backend.match.service;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.enums.ClubBookingMode;
import com.sentimospadel.backend.club.service.ClubBookingResolution;
import com.sentimospadel.backend.club.service.ClubBookingService;
import com.sentimospadel.backend.match.dto.CreateMatchRequest;
import com.sentimospadel.backend.match.dto.AssignMatchTeamsRequest;
import com.sentimospadel.backend.match.dto.MatchParticipantResponse;
import com.sentimospadel.backend.match.dto.MatchResultResponse;
import com.sentimospadel.backend.match.dto.MatchResultSummaryResponse;
import com.sentimospadel.backend.match.dto.MatchResponse;
import com.sentimospadel.backend.match.dto.MatchScoreResponse;
import com.sentimospadel.backend.match.dto.MatchTeamAssignmentRequest;
import com.sentimospadel.backend.match.dto.RejectMatchResultRequest;
import com.sentimospadel.backend.match.dto.SubmitMatchResultRequest;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.notification.service.PlayerEventNotificationService;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.player.support.UruguayCategoryMapper;
import com.sentimospadel.backend.rating.service.RatingApplicationService;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.shared.result.ResultEligibilityPolicy;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final int MAX_PLAYERS = 4;

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchResultRepository matchResultRepository;
    private final ClubBookingService clubBookingService;
    private final PlayerProfileResolverService playerProfileResolverService;
    private final RatingApplicationService ratingApplicationService;
    private final PlayerEventNotificationService playerEventNotificationService;

    @Transactional
    public MatchResponse createMatch(String email, CreateMatchRequest request) {
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);
        ClubBookingResolution bookingResolution = request.clubId() == null
                ? new ClubBookingResolution(null, null)
                : clubBookingService.resolveClubBooking(request.clubId(), request.scheduledAt(), request.locationText());
        Club club = bookingResolution.club();

        Match match = Match.builder()
                .createdBy(playerProfile)
                .status(initialStatusForBooking(bookingResolution.bookingMode()))
                .scheduledAt(request.scheduledAt())
                .club(club)
                .locationText(trimToNull(request.locationText()))
                .notes(trimToNull(request.notes()))
                .maxPlayers(MAX_PLAYERS)
                .build();

        Match savedMatch = matchRepository.save(match);
        matchParticipantRepository.save(MatchParticipant.builder()
                .match(savedMatch)
                .playerProfile(playerProfile)
                .joinedAt(Instant.now())
                .build());

        return toResponse(savedMatch);
    }

    @Transactional
    public MatchResponse joinMatch(String email, Long matchId) {
        Match match = getMatchEntity(matchId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        if (match.getStatus() == MatchStatus.CANCELLED
                || match.getStatus() == MatchStatus.PENDING_CLUB_CONFIRMATION
                || match.getStatus() == MatchStatus.RESULT_PENDING
                || match.getStatus() == MatchStatus.COMPLETED) {
            throw new ConflictException("This match cannot be joined in its current state");
        }

        if (matchParticipantRepository.existsByMatchIdAndPlayerProfileId(matchId, playerProfile.getId())) {
            throw new ConflictException("Player is already part of this match");
        }

        long participantCount = matchParticipantRepository.countByMatchId(matchId);
        if (participantCount >= match.getMaxPlayers()) {
            throw new ConflictException("Match is already full");
        }

        matchParticipantRepository.save(MatchParticipant.builder()
                .match(match)
                .playerProfile(playerProfile)
                .joinedAt(Instant.now())
                .build());

        long updatedCount = participantCount + 1;
        if (updatedCount >= match.getMaxPlayers()) {
            match.setStatus(MatchStatus.FULL);
            matchRepository.save(match);
            playerEventNotificationService.notifyMatchFull(match, getParticipants(matchId));
        }

        return toResponse(match);
    }

    @Transactional
    public MatchResponse leaveMatch(String email, Long matchId) {
        Match match = getMatchEntity(matchId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        if (match.getStatus() == MatchStatus.RESULT_PENDING || match.getStatus() == MatchStatus.COMPLETED) {
            throw new ConflictException("Completed matches cannot be modified");
        }

        MatchParticipant participant = matchParticipantRepository.findByMatchIdAndPlayerProfileId(matchId, playerProfile.getId())
                .orElseThrow(() -> new ConflictException("Player is not part of this match"));

        matchParticipantRepository.delete(participant);

        long participantCount = matchParticipantRepository.countByMatchId(matchId);
        if (match.getStatus() != MatchStatus.CANCELLED) {
            if (match.getStatus() != MatchStatus.PENDING_CLUB_CONFIRMATION) {
                match.setStatus(participantCount >= match.getMaxPlayers() ? MatchStatus.FULL : MatchStatus.OPEN);
            }
            matchRepository.save(match);
        }

        return toResponse(match);
    }

    @Transactional
    public MatchResponse assignTeams(String email, Long matchId, AssignMatchTeamsRequest request) {
        Match match = getMatchEntity(matchId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        if (!match.getCreatedBy().getId().equals(playerProfile.getId())) {
            throw new AccessDeniedException("Only the match creator can assign teams");
        }

        if (match.getStatus() == MatchStatus.CANCELLED
                || match.getStatus() == MatchStatus.PENDING_CLUB_CONFIRMATION
                || match.getStatus() == MatchStatus.RESULT_PENDING
                || match.getStatus() == MatchStatus.COMPLETED) {
            throw new ConflictException("Teams cannot be assigned in the current match state");
        }

        List<MatchParticipant> participants = getParticipants(matchId);
        validateTeamAssignments(match, participants, request);

        Map<Long, MatchParticipantTeam> assignmentByPlayerId = request.assignments()
                .stream()
                .collect(Collectors.toMap(MatchTeamAssignmentRequest::playerProfileId, MatchTeamAssignmentRequest::team));

        participants.forEach(participant -> participant.setTeam(assignmentByPlayerId.get(participant.getPlayerProfile().getId())));
        matchParticipantRepository.saveAll(participants);

        return toResponse(match);
    }

    @Transactional
    public MatchResponse cancelMatch(String email, Long matchId) {
        Match match = getMatchEntity(matchId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        if (!match.getCreatedBy().getId().equals(playerProfile.getId())) {
            throw new AccessDeniedException("Only the match creator can cancel this match");
        }

        if (match.getStatus() == MatchStatus.RESULT_PENDING || match.getStatus() == MatchStatus.COMPLETED) {
            throw new ConflictException("Matches with submitted results cannot be cancelled");
        }

        if (match.getStatus() == MatchStatus.CANCELLED) {
            return toResponse(match);
        }

        List<MatchParticipant> participants = getParticipants(matchId);
        match.setStatus(MatchStatus.CANCELLED);
        MatchResponse response = toResponse(matchRepository.save(match));
        playerEventNotificationService.notifyMatchCancelled(match, participants);
        return response;
    }

    @Transactional
    public MatchResultResponse submitResult(String email, Long matchId, SubmitMatchResultRequest request) {
        Match match = getMatchEntity(matchId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);
        Instant now = Instant.now();

        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new ConflictException("Cancelled matches cannot receive results");
        }

        if (match.getStatus() == MatchStatus.RESULT_PENDING || match.getStatus() == MatchStatus.COMPLETED) {
            throw new ConflictException("This match already has a submitted result");
        }

        if (!matchParticipantRepository.existsByMatchIdAndPlayerProfileId(matchId, playerProfile.getId())) {
            throw new ConflictException("Only match participants can submit results");
        }

        if (!ResultEligibilityPolicy.hasEnded(match.getScheduledAt(), now)) {
            throw new ConflictException("Results can only be submitted once the scheduled match has ended");
        }

        long participantCount = matchParticipantRepository.countByMatchId(matchId);
        if (participantCount < match.getMaxPlayers()) {
            throw new ConflictException("Results can only be submitted for matches with all players joined");
        }

        List<MatchParticipant> participants = getParticipants(matchId);
        validatePlayableTeams(match, participants);
        validateSubmittedResult(request);

        MatchResult savedResult = matchResultRepository.save(
                matchResultRepository.findByMatchId(matchId)
                        .map(existingResult -> prepareResubmittedResult(existingResult, playerProfile, request))
                        .orElseGet(() -> MatchResult.builder()
                                .match(match)
                                .submittedBy(playerProfile)
                                .status(MatchResultStatus.PENDING)
                                .winnerTeam(request.winnerTeam())
                                .teamOneScore(request.score().teamOneScore())
                                .teamTwoScore(request.score().teamTwoScore())
                                .submittedAt(now)
                                .build())
        );

        match.setStatus(MatchStatus.RESULT_PENDING);
        matchRepository.save(match);

        return toResultResponse(savedResult);
    }

    @Transactional
    public MatchResultResponse confirmResult(String email, Long matchId) {
        Match match = getMatchEntity(matchId);
        PlayerProfile confirmer = playerProfileResolverService.getOrCreateByUserEmail(email);
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Result for match with id " + matchId + " was not found"));

        if (result.getStatus() == MatchResultStatus.CONFIRMED) {
            ratingApplicationService.applyConfirmedResultIfNeeded(matchId);
            return toResultResponse(result);
        }

        if (result.getStatus() == MatchResultStatus.REJECTED) {
            throw new ConflictException("Rejected results must be resubmitted before they can be confirmed");
        }

        List<MatchParticipant> participants = getParticipants(matchId);
        MatchParticipant submittedByParticipant = findParticipant(participants, result.getSubmittedBy().getId());
        MatchParticipant confirmerParticipant = findParticipant(participants, confirmer.getId());

        if (confirmerParticipant == null) {
            throw new AccessDeniedException("Only match participants can confirm results");
        }

        if (result.getSubmittedBy().getId().equals(confirmer.getId())) {
            throw new AccessDeniedException("The submitting player cannot confirm their own result");
        }

        if (submittedByParticipant == null || confirmerParticipant.getTeam() == null || submittedByParticipant.getTeam() == null) {
            throw new ConflictException("Teams must be assigned before confirming a result");
        }

        if (submittedByParticipant.getTeam() == confirmerParticipant.getTeam()) {
            throw new AccessDeniedException("Result confirmation must come from the opposite team");
        }

        result.setStatus(MatchResultStatus.CONFIRMED);
        result.setConfirmedBy(confirmer);
        result.setConfirmedAt(Instant.now());
        MatchResult savedResult = matchResultRepository.save(result);

        match.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(match);
        ratingApplicationService.applyConfirmedResultIfNeeded(matchId);
        playerEventNotificationService.notifyMatchResultConfirmed(match, participants);

        return toResultResponse(savedResult);
    }

    @Transactional
    public MatchResultResponse rejectResult(String email, Long matchId, RejectMatchResultRequest request) {
        Match match = getMatchEntity(matchId);
        PlayerProfile rejector = playerProfileResolverService.getOrCreateByUserEmail(email);
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Result for match with id " + matchId + " was not found"));

        if (result.getStatus() == MatchResultStatus.CONFIRMED) {
            throw new ConflictException("Confirmed results cannot be rejected");
        }

        if (result.getStatus() == MatchResultStatus.REJECTED) {
            return toResultResponse(result);
        }

        List<MatchParticipant> participants = getParticipants(matchId);
        MatchParticipant submittedByParticipant = findParticipant(participants, result.getSubmittedBy().getId());
        MatchParticipant rejectorParticipant = findParticipant(participants, rejector.getId());

        if (rejectorParticipant == null) {
            throw new AccessDeniedException("Only match participants can reject results");
        }

        if (result.getSubmittedBy().getId().equals(rejector.getId())) {
            throw new AccessDeniedException("The submitting player cannot reject their own result");
        }

        if (submittedByParticipant == null || submittedByParticipant.getTeam() == null || rejectorParticipant.getTeam() == null) {
            throw new ConflictException("Teams must be assigned before rejecting a result");
        }

        if (submittedByParticipant.getTeam() == rejectorParticipant.getTeam()) {
            throw new AccessDeniedException("Result rejection must come from the opposite team");
        }

        result.setStatus(MatchResultStatus.REJECTED);
        result.setRejectedBy(rejector);
        result.setRejectedAt(Instant.now());
        result.setRejectionReason(trimToNull(request.rejectionReason()));
        MatchResult savedResult = matchResultRepository.save(result);

        match.setStatus(MatchStatus.FULL);
        matchRepository.save(match);
        playerEventNotificationService.notifyMatchResultRejected(match, participants, savedResult.getRejectionReason(), savedResult.getRejectedAt());

        return toResultResponse(savedResult);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatches() {
        return matchRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatchById(Long matchId) {
        return toResponse(getMatchEntity(matchId));
    }

    @Transactional(readOnly = true)
    public MatchResultResponse getMatchResult(Long matchId) {
        getMatchEntity(matchId);
        MatchResult result = matchResultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Result for match with id " + matchId + " was not found"));

        return toResultResponse(result);
    }

    private Match getMatchEntity(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match with id " + matchId + " was not found"));
    }

    private MatchResponse toResponse(Match match) {
        List<MatchParticipantResponse> participants = getParticipants(match.getId())
                .stream()
                .map(participant -> new MatchParticipantResponse(
                        participant.getPlayerProfile().getId(),
                        participant.getPlayerProfile().getUser().getId(),
                        participant.getPlayerProfile().getFullName(),
                        participant.getPlayerProfile().getCurrentRating(),
                        UruguayCategoryMapper.fromRating(participant.getPlayerProfile().getCurrentRating()),
                        participant.getPlayerProfile().getMatchesPlayed(),
                        participant.getPlayerProfile().isRequiresClubVerification(),
                        participant.getPlayerProfile().getClubVerificationStatus(),
                        participant.getTeam(),
                        participant.getJoinedAt()
                ))
                .toList();
        MatchResultSummaryResponse resultSummary = matchResultRepository.findByMatchId(match.getId())
                .map(result -> new MatchResultSummaryResponse(
                        result.getStatus(),
                        result.getWinnerTeam(),
                        new MatchScoreResponse(result.getTeamOneScore(), result.getTeamTwoScore()),
                        result.getSubmittedAt(),
                        result.getSubmittedBy().getId(),
                        result.getConfirmedBy() == null ? null : result.getConfirmedBy().getId(),
                        result.getConfirmedAt(),
                        result.getRejectedBy() == null ? null : result.getRejectedBy().getId(),
                        result.getRejectedAt(),
                        result.getRejectionReason()
                ))
                .orElse(null);

        return new MatchResponse(
                match.getId(),
                match.getCreatedBy().getId(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getClub() == null ? null : match.getClub().getId(),
                match.getLocationText(),
                match.getNotes(),
                match.getMaxPlayers(),
                participants.size(),
                resultSummary != null,
                resultSummary,
                participants,
                match.getCreatedAt(),
                match.getUpdatedAt()
        );
    }

    private MatchResultResponse toResultResponse(MatchResult result) {
        return new MatchResultResponse(
                result.getMatch().getId(),
                result.getSubmittedBy().getId(),
                result.getStatus(),
                result.getWinnerTeam(),
                new MatchScoreResponse(result.getTeamOneScore(), result.getTeamTwoScore()),
                result.getSubmittedAt(),
                result.getConfirmedBy() == null ? null : result.getConfirmedBy().getId(),
                result.getConfirmedAt(),
                result.getRejectedBy() == null ? null : result.getRejectedBy().getId(),
                result.getRejectedAt(),
                result.getRejectionReason()
        );
    }

    private MatchResult prepareResubmittedResult(
            MatchResult existingResult,
            PlayerProfile submitter,
            SubmitMatchResultRequest request
    ) {
        if (existingResult.getStatus() == MatchResultStatus.CONFIRMED || existingResult.getStatus() == MatchResultStatus.PENDING) {
            throw new ConflictException("This match already has a submitted result");
        }

        existingResult.setSubmittedBy(submitter);
        existingResult.setStatus(MatchResultStatus.PENDING);
        existingResult.setWinnerTeam(request.winnerTeam());
        existingResult.setTeamOneScore(request.score().teamOneScore());
        existingResult.setTeamTwoScore(request.score().teamTwoScore());
        existingResult.setSubmittedAt(Instant.now());
        existingResult.setConfirmedBy(null);
        existingResult.setConfirmedAt(null);
        existingResult.setRejectedBy(null);
        existingResult.setRejectedAt(null);
        existingResult.setRejectionReason(null);
        return existingResult;
    }

    private void validateSubmittedResult(SubmitMatchResultRequest request) {
        int teamOneScore = request.score().teamOneScore();
        int teamTwoScore = request.score().teamTwoScore();

        if (teamOneScore == teamTwoScore) {
            throw new BadRequestException("Match result cannot end in a tie");
        }

        if (request.winnerTeam() == MatchWinnerTeam.TEAM_ONE && teamOneScore <= teamTwoScore) {
            throw new BadRequestException("Winner team must have the higher score");
        }

        if (request.winnerTeam() == MatchWinnerTeam.TEAM_TWO && teamTwoScore <= teamOneScore) {
            throw new BadRequestException("Winner team must have the higher score");
        }
    }

    private void validateTeamAssignments(Match match, List<MatchParticipant> participants, AssignMatchTeamsRequest request) {
        if (participants.size() != match.getMaxPlayers()) {
            throw new ConflictException("Teams can only be assigned once the match has all players joined");
        }

        if (request.assignments().size() != participants.size()) {
            throw new BadRequestException("Team assignments must include every participant exactly once");
        }

        Map<Long, MatchParticipant> participantsByPlayerId = participants.stream()
                .collect(Collectors.toMap(participant -> participant.getPlayerProfile().getId(), Function.identity()));

        long teamOneCount = 0;
        long teamTwoCount = 0;

        for (MatchTeamAssignmentRequest assignment : request.assignments()) {
            if (!participantsByPlayerId.containsKey(assignment.playerProfileId())) {
                throw new BadRequestException("Team assignments contain a player that is not part of the match");
            }

            if (assignment.team() == MatchParticipantTeam.TEAM_ONE) {
                teamOneCount++;
            } else {
                teamTwoCount++;
            }
        }

        if (participantsByPlayerId.size() != request.assignments().stream().map(MatchTeamAssignmentRequest::playerProfileId).distinct().count()) {
            throw new BadRequestException("Team assignments cannot repeat the same player");
        }

        if (teamOneCount != 2 || teamTwoCount != 2) {
            throw new BadRequestException("A playable match must have exactly 2 players on each team");
        }
    }

    private void validatePlayableTeams(Match match, List<MatchParticipant> participants) {
        if (participants.size() != match.getMaxPlayers()) {
            throw new ConflictException("Match does not have enough participants to submit a result");
        }

        long teamOneCount = participants.stream().filter(participant -> participant.getTeam() == MatchParticipantTeam.TEAM_ONE).count();
        long teamTwoCount = participants.stream().filter(participant -> participant.getTeam() == MatchParticipantTeam.TEAM_TWO).count();

        if (teamOneCount != 2 || teamTwoCount != 2) {
            throw new ConflictException("Teams must be assigned as 2 versus 2 before submitting a result");
        }
    }

    private List<MatchParticipant> getParticipants(Long matchId) {
        return matchParticipantRepository.findAllByMatchIdOrderByJoinedAtAsc(matchId);
    }

    private MatchParticipant findParticipant(List<MatchParticipant> participants, Long playerProfileId) {
        return participants.stream()
                .filter(participant -> participant.getPlayerProfile().getId().equals(playerProfileId))
                .findFirst()
                .orElse(null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MatchStatus initialStatusForBooking(ClubBookingMode bookingMode) {
        if (bookingMode == ClubBookingMode.CONFIRMATION_REQUIRED) {
            return MatchStatus.PENDING_CLUB_CONFIRMATION;
        }

        return MatchStatus.OPEN;
    }
}
