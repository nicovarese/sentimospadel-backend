package com.sentimospadel.backend.tournament.service;

import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.tournament.dto.TournamentEntryMemberResponse;
import com.sentimospadel.backend.tournament.dto.TournamentEntryResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchResultResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchScoreSetResponse;
import com.sentimospadel.backend.tournament.dto.TournamentMatchTeamResponse;
import com.sentimospadel.backend.tournament.dto.TournamentResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

    public TournamentResponse toTournamentResponse(
            Tournament tournament,
            List<TournamentEntry> entries,
            long generatedMatchesCount
    ) {
        List<TournamentEntryResponse> entryResponses = entries.stream()
                .map(this::toEntryResponse)
                .toList();

        int currentPlayersCount = entries.stream()
                .mapToInt(this::memberCount)
                .sum();

        return new TournamentResponse(
                tournament.getId(),
                tournament.getCreatedBy().getId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getClub() == null ? null : tournament.getClub().getId(),
                tournament.getCity(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getStatus(),
                tournament.getFormat(),
                tournament.getAmericanoType(),
                tournament.isOpenEnrollment(),
                tournament.isCompetitive(),
                tournament.getMaxEntries(),
                entries.size(),
                currentPlayersCount,
                tournament.getAvailableCourts(),
                tournament.getNumberOfGroups(),
                tournament.getLeagueRounds(),
                tournament.getStandingsTiebreak(),
                tournament.getCourtNames() == null ? List.of() : List.copyOf(tournament.getCourtNames()),
                tournament.getLaunchedAt(),
                false,
                Math.toIntExact(generatedMatchesCount),
                entryResponses,
                tournament.getCreatedAt(),
                tournament.getUpdatedAt()
        );
    }

    public TournamentEntryResponse toEntryResponse(TournamentEntry entry) {
        return new TournamentEntryResponse(
                entry.getId(),
                displayTeamName(entry),
                entry.getStatus(),
                entry.getTimePreferences() == null ? List.of() : List.copyOf(entry.getTimePreferences()),
                toMemberResponses(entry),
                entry.getCreatedAt()
        );
    }

    public TournamentMatchResponse toMatchResponse(TournamentMatch match, TournamentMatchResult result) {
        return new TournamentMatchResponse(
                match.getId(),
                match.getTournament().getId(),
                match.getPhase(),
                match.getStatus(),
                match.getRoundNumber(),
                match.getLegNumber(),
                match.getRoundLabel(),
                match.getScheduledAt(),
                match.getCourtName(),
                toMatchTeamResponse(match.getTeamOneEntry()),
                toMatchTeamResponse(match.getTeamTwoEntry()),
                result != null,
                result == null ? null : toMatchResultResponse(result),
                match.getCreatedAt(),
                match.getUpdatedAt()
        );
    }

    public TournamentMatchResultResponse toMatchResultResponse(TournamentMatchResult result) {
        return new TournamentMatchResultResponse(
                result.getTournamentMatch().getId(),
                result.getSubmittedBy().getId(),
                result.getStatus(),
                result.getWinnerTeam(),
                toScoreSets(result),
                result.getSubmittedAt(),
                result.getConfirmedBy() == null ? null : result.getConfirmedBy().getId(),
                result.getConfirmedAt(),
                result.getRejectedBy() == null ? null : result.getRejectedBy().getId(),
                result.getRejectedAt(),
                result.getRejectionReason()
        );
    }

    public TournamentMatchTeamResponse toMatchTeamResponse(TournamentEntry entry) {
        return new TournamentMatchTeamResponse(
                entry.getId(),
                displayTeamName(entry),
                toMemberResponses(entry)
        );
    }

    public TournamentEntryMemberResponse toMemberResponse(PlayerProfile playerProfile) {
        return new TournamentEntryMemberResponse(
                playerProfile.getId(),
                playerProfile.getUser().getId(),
                playerProfile.getFullName()
        );
    }

    public String displayTeamName(TournamentEntry entry) {
        if (entry.getTeamName() != null && !entry.getTeamName().isBlank()) {
            return entry.getTeamName();
        }

        List<String> names = new ArrayList<>();
        if (entry.getPrimaryPlayerProfile() != null) {
            names.add(entry.getPrimaryPlayerProfile().getFullName());
        }
        if (entry.getSecondaryPlayerProfile() != null) {
            names.add(entry.getSecondaryPlayerProfile().getFullName());
        }
        return String.join(" / ", names);
    }

    public List<TournamentEntryMemberResponse> toMemberResponses(TournamentEntry entry) {
        List<TournamentEntryMemberResponse> members = new ArrayList<>();
        if (entry.getPrimaryPlayerProfile() != null) {
            members.add(toMemberResponse(entry.getPrimaryPlayerProfile()));
        }
        if (entry.getSecondaryPlayerProfile() != null) {
            members.add(toMemberResponse(entry.getSecondaryPlayerProfile()));
        }
        return members;
    }

    public int memberCount(TournamentEntry entry) {
        int count = 0;
        if (entry.getPrimaryPlayerProfile() != null) {
            count++;
        }
        if (entry.getSecondaryPlayerProfile() != null) {
            count++;
        }
        return count;
    }

    public List<TournamentMatchScoreSetResponse> toScoreSets(TournamentMatchResult result) {
        List<TournamentMatchScoreSetResponse> sets = new ArrayList<>();
        appendSet(sets, result.getSetOneTeamOneGames(), result.getSetOneTeamTwoGames());
        appendSet(sets, result.getSetTwoTeamOneGames(), result.getSetTwoTeamTwoGames());
        appendSet(sets, result.getSetThreeTeamOneGames(), result.getSetThreeTeamTwoGames());
        return sets;
    }

    private void appendSet(List<TournamentMatchScoreSetResponse> sets, Integer teamOneGames, Integer teamTwoGames) {
        if (teamOneGames == null || teamTwoGames == null) {
            return;
        }
        sets.add(new TournamentMatchScoreSetResponse(teamOneGames, teamTwoGames));
    }
}
