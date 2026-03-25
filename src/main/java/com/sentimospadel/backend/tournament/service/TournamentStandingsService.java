package com.sentimospadel.backend.tournament.service;

import com.sentimospadel.backend.tournament.dto.TournamentStandingsEntryResponse;
import com.sentimospadel.backend.tournament.dto.TournamentStandingsResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatchResult;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchResultStatus;
import com.sentimospadel.backend.tournament.repository.TournamentEntryRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchResultRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentStandingsService {

    private final TournamentService tournamentService;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchResultRepository tournamentMatchResultRepository;
    private final TournamentMapper tournamentMapper;

    @Transactional(readOnly = true)
    public TournamentStandingsResponse getStandings(Long tournamentId) {
        Tournament tournament = tournamentService.getTournamentEntity(tournamentId);
        if (tournament.getFormat() != TournamentFormat.LEAGUE) {
            throw new com.sentimospadel.backend.shared.exception.ConflictException(
                    "Standings are currently available only for LEAGUE tournaments"
            );
        }

        List<TournamentEntry> entries = tournamentEntryRepository.findAllByTournamentIdOrderByCreatedAtAsc(tournamentId);
        Map<Long, MutableStandingsRow> standings = new LinkedHashMap<>();
        entries.forEach(entry -> standings.put(entry.getId(), new MutableStandingsRow(entry)));

        tournamentMatchResultRepository.findAllByTournamentMatchTournamentId(tournamentId).stream()
                .filter(result -> result.getStatus() == TournamentMatchResultStatus.CONFIRMED)
                .forEach(result -> applyConfirmedResult(tournament, standings, result));

        List<MutableStandingsRow> sortedRows = standings.values().stream()
                .sorted(Comparator
                        .comparingInt(MutableStandingsRow::points).reversed()
                        .thenComparingInt(MutableStandingsRow::gamesDifference).reversed()
                        .thenComparingInt(MutableStandingsRow::gamesWon).reversed()
                        .thenComparingInt(MutableStandingsRow::setDifference).reversed()
                        .thenComparing(row -> tournamentMapper.displayTeamName(row.entry())))
                .toList();

        List<TournamentStandingsEntryResponse> responseRows = java.util.stream.IntStream.range(0, sortedRows.size())
                .mapToObj(index -> sortedRows.get(index).toResponse(index + 1, tournamentMapper))
                .toList();

        return new TournamentStandingsResponse(
                tournament.getId(),
                tournament.getStandingsTiebreak(),
                responseRows
        );
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
