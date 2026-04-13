package com.sentimospadel.backend.notification.service;

import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.notification.enums.PendingActionType;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.verification.entity.ClubVerificationRequest;
import com.sentimospadel.backend.verification.enums.ClubVerificationRequestStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerEventNotificationService {

    private final PlayerNotificationService playerNotificationService;

    public void notifyMatchFull(Match match, List<MatchParticipant> participants) {
        String actionKey = "event-match-full-" + match.getId() + "-" + Instant.now().toEpochMilli();
        publishToPlayers(
                playersFromParticipants(participants),
                PendingActionType.MATCH_FULL,
                actionKey,
                "Tu partido ya esta completo",
                "El partido en " + socialLocationLabel(match) + " completo sus 4 jugadores.",
                match.getId(),
                null,
                null
        );
    }

    public void notifyMatchCancelled(Match match, List<MatchParticipant> participants) {
        publishToPlayers(
                playersFromParticipants(participants),
                PendingActionType.MATCH_CANCELLED,
                "event-match-cancelled-" + match.getId(),
                "Se cancelo tu partido",
                "El partido en " + socialLocationLabel(match) + " fue cancelado por el organizador.",
                match.getId(),
                null,
                null
        );
    }

    public void notifyMatchResultConfirmed(Match match, List<MatchParticipant> participants) {
        publishToPlayers(
                playersFromParticipants(participants),
                PendingActionType.MATCH_RESULT_CONFIRMED,
                "event-match-result-confirmed-" + match.getId(),
                "Resultado confirmado",
                "El resultado del partido en " + socialLocationLabel(match) + " ya fue confirmado.",
                match.getId(),
                null,
                null
        );
    }

    public void notifyMatchResultRejected(Match match, List<MatchParticipant> participants, String rejectionReason, Instant rejectedAt) {
        publishToPlayers(
                playersFromParticipants(participants),
                PendingActionType.MATCH_RESULT_REJECTED,
                "event-match-result-rejected-" + match.getId() + "-" + rejectedAt.toEpochMilli(),
                "Resultado rechazado",
                "El resultado del partido en " + socialLocationLabel(match) + " fue rechazado." + noteSuffix(rejectionReason),
                match.getId(),
                null,
                null
        );
    }

    public void notifyTournamentLaunched(Tournament tournament, List<TournamentEntry> entries) {
        publishToPlayers(
                playersFromEntries(entries),
                PendingActionType.TOURNAMENT_LAUNCHED,
                "event-tournament-launched-" + tournament.getId(),
                "Tu torneo ya fue lanzado",
                "El torneo " + tournament.getName() + " ya fue lanzado y tiene partidos agendados.",
                null,
                tournament.getId(),
                null
        );
    }

    public void notifyTournamentResultConfirmed(TournamentMatch match) {
        publishToPlayers(
                playersFromEntries(List.of(match.getTeamOneEntry(), match.getTeamTwoEntry())),
                PendingActionType.TOURNAMENT_RESULT_CONFIRMED,
                "event-tournament-result-confirmed-" + match.getId(),
                "Resultado confirmado en torneo",
                "El resultado de " + match.getTournament().getName() + " para " + match.getRoundLabel() + " ya fue confirmado.",
                null,
                match.getTournament().getId(),
                match.getId()
        );
    }

    public void notifyTournamentResultRejected(TournamentMatch match, String rejectionReason, Instant rejectedAt) {
        publishToPlayers(
                playersFromEntries(List.of(match.getTeamOneEntry(), match.getTeamTwoEntry())),
                PendingActionType.TOURNAMENT_RESULT_REJECTED,
                "event-tournament-result-rejected-" + match.getId() + "-" + rejectedAt.toEpochMilli(),
                "Resultado rechazado en torneo",
                "El resultado de " + match.getTournament().getName() + " para " + match.getRoundLabel() + " fue rechazado." + noteSuffix(rejectionReason),
                null,
                match.getTournament().getId(),
                match.getId()
        );
    }

    public void notifyClubBookingApproved(Match match, List<MatchParticipant> participants) {
        publishToPlayers(
                playersFromParticipants(participants),
                PendingActionType.CLUB_BOOKING_APPROVED,
                "event-club-booking-approved-" + match.getId(),
                "Reserva aprobada",
                "Tu reserva en " + socialLocationLabel(match) + " fue aprobada por el club.",
                match.getId(),
                null,
                null
        );
    }

    public void notifyClubBookingRejected(Match match, List<MatchParticipant> participants) {
        publishToPlayers(
                playersFromParticipants(participants),
                PendingActionType.CLUB_BOOKING_REJECTED,
                "event-club-booking-rejected-" + match.getId(),
                "Reserva rechazada",
                "Tu reserva en " + socialLocationLabel(match) + " fue rechazada por el club.",
                match.getId(),
                null,
                null
        );
    }

    public void notifyClubVerificationDecision(ClubVerificationRequest request) {
        ClubVerificationRequestStatus status = request.getStatus();
        if (status != ClubVerificationRequestStatus.APPROVED && status != ClubVerificationRequestStatus.REJECTED) {
            return;
        }

        boolean approved = status == ClubVerificationRequestStatus.APPROVED;
        playerNotificationService.publishEventNotification(
                request.getPlayerProfile(),
                approved ? PendingActionType.CLUB_VERIFICATION_APPROVED : PendingActionType.CLUB_VERIFICATION_REJECTED,
                "event-club-verification-" + request.getId() + "-" + status.name().toLowerCase(),
                approved ? "Verificacion aprobada" : "Verificacion rechazada",
                approved
                        ? "El club " + request.getClub().getName() + " valido tu categoria." + noteSuffix(request.getReviewNotes())
                        : "El club " + request.getClub().getName() + " rechazo tu verificacion." + noteSuffix(request.getReviewNotes()),
                null,
                null,
                null
        );
    }

    private void publishToPlayers(
            Collection<PlayerProfile> players,
            PendingActionType type,
            String actionKey,
            String title,
            String message,
            Long matchId,
            Long tournamentId,
            Long tournamentMatchId
    ) {
        for (PlayerProfile player : players) {
            playerNotificationService.publishEventNotification(
                    player,
                    type,
                    actionKey,
                    title,
                    message,
                    matchId,
                    tournamentId,
                    tournamentMatchId
            );
        }
    }

    private List<PlayerProfile> playersFromParticipants(List<MatchParticipant> participants) {
        return distinctPlayers(participants.stream()
                .map(MatchParticipant::getPlayerProfile)
                .toList());
    }

    private List<PlayerProfile> playersFromEntries(List<TournamentEntry> entries) {
        return distinctPlayers(entries.stream()
                .flatMap(entry -> java.util.stream.Stream.of(entry.getPrimaryPlayerProfile(), entry.getSecondaryPlayerProfile()))
                .filter(Objects::nonNull)
                .toList());
    }

    private List<PlayerProfile> distinctPlayers(List<PlayerProfile> players) {
        Map<Long, PlayerProfile> playersById = new LinkedHashMap<>();
        for (PlayerProfile player : players) {
            playersById.putIfAbsent(player.getId(), player);
        }
        return List.copyOf(playersById.values());
    }

    private String socialLocationLabel(Match match) {
        if (match.getLocationText() != null && !match.getLocationText().isBlank()) {
            return match.getLocationText().trim();
        }
        if (match.getClub() != null && match.getClub().getName() != null && !match.getClub().getName().isBlank()) {
            return match.getClub().getName().trim();
        }
        return "la sede acordada";
    }

    private String noteSuffix(String note) {
        String trimmed = note == null ? null : note.trim();
        if (trimmed == null || trimmed.isBlank()) {
            return "";
        }
        return " Nota: " + trimmed;
    }
}
