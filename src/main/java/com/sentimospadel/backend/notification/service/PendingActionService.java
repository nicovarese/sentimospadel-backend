package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.shared.result.ResultEligibilityPolicy;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PendingActionService {

    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchResultRepository matchResultRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentMatchResultRepository tournamentMatchResultRepository;

    @Transactional(readOnly = true)
    public List<PendingActionCandidate> computePendingActions(Long playerProfileId) {
        Instant now = Instant.now();
        List<PendingActionCandidate> candidates = new ArrayList<>();
        candidates.addAll(computeSocialMatchActions(playerProfileId, now));
        candidates.addAll(computeTournamentActions(playerProfileId, now));

        return candidates.stream()
                .sorted((left, right) -> {
                    int dueDateComparison = left.dueAt().compareTo(right.dueAt());
                    if (dueDateComparison != 0) {
                        return dueDateComparison;
                    }
                    return left.title().compareToIgnoreCase(right.title());
                })
                .toList();
    }

    private List<PendingActionCandidate> computeSocialMatchActions(Long playerProfileId, Instant now) {
        List<MatchParticipant> participations =
                matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(playerProfileId);
        if (participations.isEmpty()) {
            return List.of();
        }

        List<Long> matchIds = participations.stream()
                .map(participation -> participation.getMatch().getId())
                .distinct()
                .toList();

        Map<Long, List<MatchParticipant>> participantsByMatchId = matchParticipantRepository
                .findAllByMatchIdInOrderByJoinedAtAsc(matchIds)
                .stream()
                .collect(Collectors.groupingBy(participant -> participant.getMatch().getId()));

        Map<Long, MatchResult> resultsByMatchId = matchResultRepository.findAllByMatchIdIn(matchIds)
                .stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), Function.identity()));

        List<PendingActionCandidate> candidates = new ArrayList<>();
        for (MatchParticipant participation : participations) {
            Match match = participation.getMatch();
            List<MatchParticipant> participants = participantsByMatchId.getOrDefault(match.getId(), List.of());
            MatchResult result = resultsByMatchId.get(match.getId());
            Instant dueAt = ResultEligibilityPolicy.resultEligibleAt(match.getScheduledAt());

            if (isSocialMatchReadyForSubmission(match, participants, result, now)) {
                candidates.add(new PendingActionCandidate(
                        "submit-match-" + match.getId(),
                        PendingActionType.SUBMIT_MATCH_RESULT,
                        match.getId(),
                        null,
                        null,
                        "Carga el resultado del partido",
                        buildSocialSubmitMessage(match),
                        match.getScheduledAt(),
                        dueAt
                ));
                continue;
            }

            if (isSocialMatchReadyForConfirmation(match, participants, result, playerProfileId, now)) {
                candidates.add(new PendingActionCandidate(
                        "confirm-match-" + match.getId(),
                        PendingActionType.CONFIRM_MATCH_RESULT,
                        match.getId(),
                        null,
                        null,
                        "Confirmá el resultado del partido",
                        buildSocialConfirmMessage(match),
                        match.getScheduledAt(),
                        result.getSubmittedAt()
                ));
            }
        }

        return deduplicateByActionKey(candidates);
    }

    private List<PendingActionCandidate> computeTournamentActions(Long playerProfileId, Instant now) {
        List<TournamentMatch> matches = tournamentMatchRepository.findAllByParticipantPlayerProfileIdOrderByScheduledAtDesc(playerProfileId);
        if (matches.isEmpty()) {
            return List.of();
        }

        List<Long> matchIds = matches.stream().map(TournamentMatch::getId).toList();
        Map<Long, TournamentMatchResult> resultsByMatchId = tournamentMatchResultRepository.findAllByTournamentMatchIdIn(matchIds)
                .stream()
                .collect(Collectors.toMap(result -> result.getTournamentMatch().getId(), Function.identity()));

        List<PendingActionCandidate> candidates = new ArrayList<>();
        for (TournamentMatch match : matches) {
            TournamentMatchResult result = resultsByMatchId.get(match.getId());
            Instant dueAt = ResultEligibilityPolicy.resultEligibleAt(match.getScheduledAt());

            if (isTournamentMatchReadyForSubmission(match, result, now, playerProfileId)) {
                candidates.add(new PendingActionCandidate(
                        "submit-tournament-match-" + match.getId(),
                        PendingActionType.SUBMIT_TOURNAMENT_RESULT,
                        null,
                        match.getTournament().getId(),
                        match.getId(),
                        "Carga el resultado del torneo",
                        buildTournamentSubmitMessage(match),
                        match.getScheduledAt(),
                        dueAt
                ));
                continue;
            }

            if (isTournamentMatchReadyForConfirmation(match, result, now, playerProfileId)) {
                candidates.add(new PendingActionCandidate(
                        "confirm-tournament-match-" + match.getId(),
                        PendingActionType.CONFIRM_TOURNAMENT_RESULT,
                        null,
                        match.getTournament().getId(),
                        match.getId(),
                        "Confirmá el resultado del torneo",
                        buildTournamentConfirmMessage(match),
                        match.getScheduledAt(),
                        result.getSubmittedAt()
                ));
            }
        }

        return deduplicateByActionKey(candidates);
    }

    private boolean isSocialMatchReadyForSubmission(
            Match match,
            List<MatchParticipant> participants,
            MatchResult result,
            Instant now
    ) {
        return match.getStatus() == MatchStatus.FULL
                && ResultEligibilityPolicy.hasEnded(match.getScheduledAt(), now)
                && participants.size() == match.getMaxPlayers()
                && hasPlayableTeams(participants)
                && (result == null || result.getStatus() == MatchResultStatus.REJECTED);
    }

    private boolean isSocialMatchReadyForConfirmation(
            Match match,
            List<MatchParticipant> participants,
            MatchResult result,
            Long playerProfileId,
            Instant now
    ) {
        if (match.getStatus() != MatchStatus.RESULT_PENDING
                || result == null
                || result.getStatus() != MatchResultStatus.PENDING
                || !ResultEligibilityPolicy.hasEnded(match.getScheduledAt(), now)) {
            return false;
        }

        MatchParticipant submittedByParticipant = participants.stream()
                .filter(participant -> participant.getPlayerProfile().getId().equals(result.getSubmittedBy().getId()))
                .findFirst()
                .orElse(null);
        MatchParticipant actorParticipant = participants.stream()
                .filter(participant -> participant.getPlayerProfile().getId().equals(playerProfileId))
                .findFirst()
                .orElse(null);

        return actorParticipant != null
                && !result.getSubmittedBy().getId().equals(playerProfileId)
                && actorParticipant.getTeam() != null
                && submittedByParticipant != null
                && submittedByParticipant.getTeam() != null
                && actorParticipant.getTeam() != submittedByParticipant.getTeam();
    }

    private boolean isTournamentMatchReadyForSubmission(
            TournamentMatch match,
            TournamentMatchResult result,
            Instant now,
            Long playerProfileId
    ) {
        return match.getStatus() == TournamentMatchStatus.SCHEDULED
                && (match.getTournament().getStatus() == TournamentStatus.IN_PROGRESS
                        || match.getTournament().getStatus() == TournamentStatus.COMPLETED)
                && ResultEligibilityPolicy.hasEnded(match.getScheduledAt(), now)
                && hasConfirmedTournamentTeams(match)
                && isPlayerInTournamentMatch(match, playerProfileId)
                && (result == null || result.getStatus() == TournamentMatchResultStatus.REJECTED);
    }

    private boolean isTournamentMatchReadyForConfirmation(
            TournamentMatch match,
            TournamentMatchResult result,
            Instant now,
            Long playerProfileId
    ) {
        if (match.getStatus() != TournamentMatchStatus.RESULT_PENDING
                || result == null
                || result.getStatus() != TournamentMatchResultStatus.PENDING
                || !ResultEligibilityPolicy.hasEnded(match.getScheduledAt(), now)) {
            return false;
        }

        TournamentEntry submittedByTeam = tournamentTeamForPlayer(match, result.getSubmittedBy().getId());
        TournamentEntry actorTeam = tournamentTeamForPlayer(match, playerProfileId);

        return actorTeam != null
                && submittedByTeam != null
                && !Objects.equals(result.getSubmittedBy().getId(), playerProfileId)
                && !Objects.equals(actorTeam.getId(), submittedByTeam.getId());
    }

    private boolean hasPlayableTeams(List<MatchParticipant> participants) {
        long teamOneCount = participants.stream()
                .filter(participant -> participant.getTeam() == MatchParticipantTeam.TEAM_ONE)
                .count();
        long teamTwoCount = participants.stream()
                .filter(participant -> participant.getTeam() == MatchParticipantTeam.TEAM_TWO)
                .count();
        return teamOneCount == 2 && teamTwoCount == 2;
    }

    private boolean hasConfirmedTournamentTeams(TournamentMatch match) {
        return match.getTeamOneEntry().getStatus() == TournamentEntryStatus.CONFIRMED
                && match.getTeamTwoEntry().getStatus() == TournamentEntryStatus.CONFIRMED;
    }

    private boolean isPlayerInTournamentMatch(TournamentMatch match, Long playerProfileId) {
        return tournamentTeamForPlayer(match, playerProfileId) != null;
    }

    private TournamentEntry tournamentTeamForPlayer(TournamentMatch match, Long playerProfileId) {
        if (isPlayerInTournamentEntry(match.getTeamOneEntry(), playerProfileId)) {
            return match.getTeamOneEntry();
        }
        if (isPlayerInTournamentEntry(match.getTeamTwoEntry(), playerProfileId)) {
            return match.getTeamTwoEntry();
        }
        return null;
    }

    private boolean isPlayerInTournamentEntry(TournamentEntry entry, Long playerProfileId) {
        return entry.getPrimaryPlayerProfile() != null && entry.getPrimaryPlayerProfile().getId().equals(playerProfileId)
                || entry.getSecondaryPlayerProfile() != null && entry.getSecondaryPlayerProfile().getId().equals(playerProfileId);
    }

    private String buildSocialSubmitMessage(Match match) {
        return "Tu partido en " + socialLocationLabel(match) + " ya terminó. Cargá el resultado.";
    }

    private String buildSocialConfirmMessage(Match match) {
        return "Tu rival cargó el resultado del partido en " + socialLocationLabel(match) + ". Confirmalo o rechazalo.";
    }

    private String buildTournamentSubmitMessage(TournamentMatch match) {
        return "El partido de " + match.getTournament().getName() + " ya terminó. Cargá el resultado.";
    }

    private String buildTournamentConfirmMessage(TournamentMatch match) {
        return "El rival cargó el resultado de " + match.getTournament().getName() + ". Confirmalo o rechazalo.";
    }

    private String socialLocationLabel(Match match) {
        if (match.getLocationText() != null && !match.getLocationText().isBlank()) {
            return match.getLocationText().trim();
        }
        if (match.getClub() != null && match.getClub().getName() != null) {
            return match.getClub().getName();
        }
        return "la sede acordada";
    }

    private List<PendingActionCandidate> deduplicateByActionKey(List<PendingActionCandidate> candidates) {
        return new ArrayList<>(candidates.stream()
                .collect(Collectors.toMap(
                        PendingActionCandidate::actionKey,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ))
                .values());
    }
}
