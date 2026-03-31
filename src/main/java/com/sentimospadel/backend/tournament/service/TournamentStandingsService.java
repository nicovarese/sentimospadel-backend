package com.sentimospadel.backend.tournament.service;

import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsEntryResponse;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsGroupResponse;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentAmericanoType;
import com.sentimospadel.backend.tournament.enums.TournamentEntryKind;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.repository.TournamentEntryRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentStandingsService {

    private final TournamentService tournamentService;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final TournamentMatchResultRepository tournamentMatchResultRepository;
    private final TournamentMapper tournamentMapper;

    @Transactional(readOnly = true)
    public TournamentStandingsResponse getStandings(Long tournamentId) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        List<TournamentEntry> entries = tournamentEntryRepository.findAllByTournamentIdAndEntryKindOrderByCreatedAtAsc(
                tournamentId,
                TournamentEntryKind.REGISTERED
        );
        List<TournamentMatchResult> confirmedResults = tournamentMatchResultRepository.findAllByTournamentMatchTournamentId(tournamentId).stream()
                .filter(result -> result.getStatus() == TournamentMatchResultStatus.CONFIRMED)
                .toList();

        if (tournament.getFormat() == TournamentFormat.LEAGUE
                || (tournament.getFormat() == TournamentFormat.AMERICANO
                && tournament.getAmericanoType() == TournamentAmericanoType.FIXED)) {
            List<TournamentStandingsEntryResponse> standings = buildLeagueStandings(tournament, entries, confirmedResults);
            return new TournamentStandingsResponse(
                    tournament.getId(),
                    tournament.getStandingsTiebreak(),
                    standings,
                    List.of()
            );
        }

        if (tournament.getFormat() == TournamentFormat.ELIMINATION) {
            List<TournamentStandingsGroupResponse> groups = buildEliminationGroups(tournament, entries, confirmedResults);
            return new TournamentStandingsResponse(
                    tournament.getId(),
                    tournament.getStandingsTiebreak(),
                    List.of(),
                    groups
            );
        }

        if (tournament.getFormat() == TournamentFormat.AMERICANO
                && tournament.getAmericanoType() == TournamentAmericanoType.DYNAMIC) {
            List<TournamentStandingsEntryResponse> standings = buildDynamicAmericanoStandings(entries, confirmedResults, tournament.getMatchesPerParticipant());
            return new TournamentStandingsResponse(
                    tournament.getId(),
                    tournament.getStandingsTiebreak(),
                    standings,
                    List.of()
            );
        }

        throw new ConflictException("Standings are currently available only for LEAGUE, ELIMINATION and operational AMERICANO tournaments");
    }

    @Transactional(readOnly = true)
    public List<TournamentStandingsGroupResponse> buildEliminationGroups(Tournament tournament) {
        List<TournamentEntry> entries = tournamentEntryRepository.findAllByTournamentIdAndEntryKindOrderByCreatedAtAsc(
                tournament.getId(),
                TournamentEntryKind.REGISTERED
        );
        List<TournamentMatchResult> confirmedResults = tournamentMatchResultRepository.findAllByTournamentMatchTournamentId(tournament.getId()).stream()
                .filter(result -> result.getStatus() == TournamentMatchResultStatus.CONFIRMED)
                .toList();
        return buildEliminationGroups(tournament, entries, confirmedResults);
    }

    private List<TournamentStandingsEntryResponse> buildLeagueStandings(
            Tournament tournament,
            List<TournamentEntry> entries,
            List<TournamentMatchResult> confirmedResults
    ) {
        Map<Long, MutableStandingsRow> standings = new LinkedHashMap<>();
        entries.forEach(entry -> standings.put(entry.getId(), new MutableStandingsRow(entry)));

        confirmedResults.forEach(result -> applyConfirmedResult(tournament, standings, result));

        List<MutableStandingsRow> sortedRows = sortRows(standings.values().stream().toList());
        return toResponses(sortedRows);
    }

    private List<TournamentStandingsGroupResponse> buildEliminationGroups(
            Tournament tournament,
            List<TournamentEntry> entries,
            List<TournamentMatchResult> confirmedResults
    ) {
        List<TournamentMatch> matches = tournamentMatchRepository.findAllByTournamentIdOrderByRoundNumberAscIdAsc(tournament.getId());
        Map<Long, TournamentMatchResult> confirmedResultByMatchId = confirmedResults.stream()
                .collect(LinkedHashMap::new, (map, result) -> map.put(result.getTournamentMatch().getId(), result), Map::putAll);

        Map<String, List<TournamentEntry>> entriesByGroup = new LinkedHashMap<>();
        for (TournamentEntry entry : entries) {
            String groupLabel = entry.getGroupLabel();
            if (groupLabel == null || groupLabel.isBlank()) {
                continue;
            }
            entriesByGroup.computeIfAbsent(groupLabel, ignored -> new ArrayList<>()).add(entry);
        }

        List<TournamentStandingsGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<String, List<TournamentEntry>> group : entriesByGroup.entrySet()) {
            Map<Long, MutableStandingsRow> standings = new LinkedHashMap<>();
            group.getValue().forEach(entry -> standings.put(entry.getId(), new MutableStandingsRow(entry)));

            matches.stream()
                    .filter(match -> match.getPhase() == TournamentMatchPhase.GROUP_STAGE)
                    .filter(match -> Objects.equals(match.getTeamOneEntry().getGroupLabel(), group.getKey()))
                    .forEach(match -> {
                        TournamentMatchResult result = confirmedResultByMatchId.get(match.getId());
                        if (result != null) {
                            applyConfirmedResult(tournament, standings, result);
                        }
                    });

            List<MutableStandingsRow> sortedRows = sortRows(standings.values().stream().toList());
            groups.add(new TournamentStandingsGroupResponse(group.getKey(), toResponses(sortedRows)));
        }

        return groups;
    }

    private List<TournamentStandingsEntryResponse> buildDynamicAmericanoStandings(
            List<TournamentEntry> registeredEntries,
            List<TournamentMatchResult> confirmedResults,
            Integer matchesPerParticipant
    ) {
        Map<Long, MutableStandingsRow> standingsByPlayerId = new LinkedHashMap<>();
        registeredEntries.forEach(entry -> {
            if (entry.getPrimaryPlayerProfile() != null) {
                standingsByPlayerId.put(entry.getPrimaryPlayerProfile().getId(), new MutableStandingsRow(entry));
            }
        });

        Map<Long, Integer> countedMatchesByPlayerId = new LinkedHashMap<>();
        int matchLimit = matchesPerParticipant == null ? Integer.MAX_VALUE : matchesPerParticipant;

        confirmedResults.stream()
                .sorted(Comparator
                        .comparing((TournamentMatchResult result) -> result.getTournamentMatch().getScheduledAt(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(result -> result.getTournamentMatch().getId()))
                .forEach(result -> applyDynamicAmericanoResult(standingsByPlayerId, countedMatchesByPlayerId, result, matchLimit));

        List<MutableStandingsRow> sortedRows = standingsByPlayerId.values().stream()
                .sorted(Comparator
                        .comparingInt(MutableStandingsRow::points).reversed()
                        .thenComparingInt(MutableStandingsRow::gamesDifference).reversed()
                        .thenComparingInt(MutableStandingsRow::gamesWon).reversed()
                        .thenComparing(row -> tournamentMapper.displayTeamName(row.entry())))
                .toList();
        return toResponses(sortedRows);
    }

    private void applyDynamicAmericanoResult(
            Map<Long, MutableStandingsRow> standingsByPlayerId,
            Map<Long, Integer> countedMatchesByPlayerId,
            TournamentMatchResult result,
            int matchesPerParticipant
    ) {
        TournamentMatch match = result.getTournamentMatch();

        int teamOneSetsWon = 0;
        int teamTwoSetsWon = 0;
        int teamOneGamesWon = 0;
        int teamTwoGamesWon = 0;

        List<int[]> sets = List.of(
                toSet(result.getSetOneTeamOneGames(), result.getSetOneTeamTwoGames()),
                toSet(result.getSetTwoTeamOneGames(), result.getSetTwoTeamTwoGames()),
                toSet(result.getSetThreeTeamOneGames(), result.getSetThreeTeamTwoGames())
        );

        for (int[] set : sets) {
            if (set == null) {
                continue;
            }
            teamOneGamesWon += set[0];
            teamTwoGamesWon += set[1];
            if (set[0] > set[1]) {
                teamOneSetsWon++;
            } else {
                teamTwoSetsWon++;
            }
        }

        applyDynamicPlayerRow(
                standingsByPlayerId,
                countedMatchesByPlayerId,
                playerId(match.getTeamOneEntry(), true),
                result.getWinnerTeam() == com.sentimospadel.backend.match.enums.MatchWinnerTeam.TEAM_ONE,
                teamOneSetsWon,
                teamTwoSetsWon,
                teamOneGamesWon,
                teamTwoGamesWon,
                matchesPerParticipant
        );
        applyDynamicPlayerRow(
                standingsByPlayerId,
                countedMatchesByPlayerId,
                playerId(match.getTeamOneEntry(), false),
                result.getWinnerTeam() == com.sentimospadel.backend.match.enums.MatchWinnerTeam.TEAM_ONE,
                teamOneSetsWon,
                teamTwoSetsWon,
                teamOneGamesWon,
                teamTwoGamesWon,
                matchesPerParticipant
        );
        applyDynamicPlayerRow(
                standingsByPlayerId,
                countedMatchesByPlayerId,
                playerId(match.getTeamTwoEntry(), true),
                result.getWinnerTeam() == com.sentimospadel.backend.match.enums.MatchWinnerTeam.TEAM_TWO,
                teamTwoSetsWon,
                teamOneSetsWon,
                teamTwoGamesWon,
                teamOneGamesWon,
                matchesPerParticipant
        );
        applyDynamicPlayerRow(
                standingsByPlayerId,
                countedMatchesByPlayerId,
                playerId(match.getTeamTwoEntry(), false),
                result.getWinnerTeam() == com.sentimospadel.backend.match.enums.MatchWinnerTeam.TEAM_TWO,
                teamTwoSetsWon,
                teamOneSetsWon,
                teamTwoGamesWon,
                teamOneGamesWon,
                matchesPerParticipant
        );
    }

    private void applyDynamicPlayerRow(
            Map<Long, MutableStandingsRow> standingsByPlayerId,
            Map<Long, Integer> countedMatchesByPlayerId,
            Long playerProfileId,
            boolean won,
            int setsWon,
            int setsLost,
            int gamesWon,
            int gamesLost,
            int matchesPerParticipant
    ) {
        if (playerProfileId == null) {
            return;
        }

        int alreadyCounted = countedMatchesByPlayerId.getOrDefault(playerProfileId, 0);
        if (alreadyCounted >= matchesPerParticipant) {
            return;
        }

        MutableStandingsRow row = standingsByPlayerId.get(playerProfileId);
        if (row == null) {
            return;
        }

        countedMatchesByPlayerId.put(playerProfileId, alreadyCounted + 1);
        row.played++;
        row.gamesWon += gamesWon;
        row.gamesLost += gamesLost;
        row.setsWon += setsWon;
        row.setsLost += setsLost;

        if (won) {
            row.wins++;
            row.points += 2;
        } else {
            row.losses++;
        }
    }

    private Long playerId(TournamentEntry entry, boolean primary) {
        if (primary) {
            return entry.getPrimaryPlayerProfile() == null ? null : entry.getPrimaryPlayerProfile().getId();
        }
        return entry.getSecondaryPlayerProfile() == null ? null : entry.getSecondaryPlayerProfile().getId();
    }

    private List<MutableStandingsRow> sortRows(List<MutableStandingsRow> rows) {
        return rows.stream()
                .sorted(Comparator
                        .comparingInt(MutableStandingsRow::points).reversed()
                        .thenComparingInt(MutableStandingsRow::gamesDifference).reversed()
                        .thenComparingInt(MutableStandingsRow::gamesWon).reversed()
                        .thenComparingInt(MutableStandingsRow::setDifference).reversed()
                        .thenComparing(row -> tournamentMapper.displayTeamName(row.entry())))
                .toList();
    }

    private List<TournamentStandingsEntryResponse> toResponses(List<MutableStandingsRow> rows) {
        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> rows.get(index).toResponse(index + 1, tournamentMapper))
                .toList();
    }

    private void applyConfirmedResult(
            Tournament tournament,
            Map<Long, MutableStandingsRow> standings,
            TournamentMatchResult result
    ) {
        MutableStandingsRow teamOne = standings.get(result.getTournamentMatch().getTeamOneEntry().getId());
        MutableStandingsRow teamTwo = standings.get(result.getTournamentMatch().getTeamTwoEntry().getId());
        if (teamOne == null || teamTwo == null) {
            return;
        }

        int teamOneSetsWon = 0;
        int teamTwoSetsWon = 0;
        int teamOneGamesWon = 0;
        int teamTwoGamesWon = 0;

        List<int[]> sets = List.of(
                toSet(result.getSetOneTeamOneGames(), result.getSetOneTeamTwoGames()),
                toSet(result.getSetTwoTeamOneGames(), result.getSetTwoTeamTwoGames()),
                toSet(result.getSetThreeTeamOneGames(), result.getSetThreeTeamTwoGames())
        );

        for (int[] set : sets) {
            if (set == null) {
                continue;
            }
            teamOneGamesWon += set[0];
            teamTwoGamesWon += set[1];
            if (set[0] > set[1]) {
                teamOneSetsWon++;
            } else {
                teamTwoSetsWon++;
            }
        }

        teamOne.played++;
        teamTwo.played++;
        teamOne.gamesWon += teamOneGamesWon;
        teamOne.gamesLost += teamTwoGamesWon;
        teamTwo.gamesWon += teamTwoGamesWon;
        teamTwo.gamesLost += teamOneGamesWon;
        teamOne.setsWon += teamOneSetsWon;
        teamOne.setsLost += teamTwoSetsWon;
        teamTwo.setsWon += teamTwoSetsWon;
        teamTwo.setsLost += teamOneSetsWon;

        if (teamOneSetsWon > teamTwoSetsWon) {
            teamOne.wins++;
            teamTwo.losses++;
            teamOne.points += tournament.getPointsForWin();
            teamTwo.points += teamTwoSetsWon > 0 ? tournament.getPointsForTiebreakLoss() : tournament.getPointsForLoss();
        } else {
            teamTwo.wins++;
            teamOne.losses++;
            teamTwo.points += tournament.getPointsForWin();
            teamOne.points += teamOneSetsWon > 0 ? tournament.getPointsForTiebreakLoss() : tournament.getPointsForLoss();
        }
    }

    private int[] toSet(Integer teamOneGames, Integer teamTwoGames) {
        if (teamOneGames == null || teamTwoGames == null) {
            return null;
        }
        return new int[]{teamOneGames, teamTwoGames};
    }

    private static final class MutableStandingsRow {
        private final TournamentEntry entry;
        private int points;
        private int played;
        private int wins;
        private int losses;
        private int setsWon;
        private int setsLost;
        private int gamesWon;
        private int gamesLost;

        private MutableStandingsRow(TournamentEntry entry) {
            this.entry = entry;
        }

        private TournamentEntry entry() {
            return entry;
        }

        private int points() {
            return points;
        }

        private int gamesDifference() {
            return gamesWon - gamesLost;
        }

        private int gamesWon() {
            return gamesWon;
        }

        private int setDifference() {
            return setsWon - setsLost;
        }

        private TournamentStandingsEntryResponse toResponse(int position, TournamentMapper mapper) {
            return new TournamentStandingsEntryResponse(
                    position,
                    entry.getId(),
                    mapper.displayTeamName(entry),
                    mapper.toMemberResponses(entry),
                    points,
                    played,
                    wins,
                    losses,
                    setsWon,
                    setsLost,
                    setDifference(),
                    gamesWon,
                    gamesLost,
                    gamesDifference()
            );
        }
    }
}
