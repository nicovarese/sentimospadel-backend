package com.sentimospadel.backend.tournament.service;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.tournament.dto.CreateTournamentRequest;
import com.sentimospadel.backend.tournament.dto.LaunchTournamentRequest;
import com.sentimospadel.backend.tournament.dto.SyncTournamentEntriesRequest;
import com.sentimospadel.backend.tournament.dto.TournamentEntryUpsertRequest;
import com.sentimospadel.backend.tournament.dto.TournamentResponse;
import com.sentimospadel.backend.tournament.entity.Tournament;
import com.sentimospadel.backend.tournament.entity.TournamentEntry;
import com.sentimospadel.backend.tournament.entity.TournamentMatch;
import com.sentimospadel.backend.tournament.enums.TournamentAmericanoType;
import com.sentimospadel.backend.tournament.enums.TournamentEntryKind;
import com.sentimospadel.backend.tournament.enums.TournamentEntryStatus;
import com.sentimospadel.backend.tournament.enums.TournamentFormat;
import com.sentimospadel.backend.tournament.enums.TournamentMatchPhase;
import com.sentimospadel.backend.tournament.enums.TournamentMatchStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStatus;
import com.sentimospadel.backend.tournament.enums.TournamentStandingsTiebreak;
import com.sentimospadel.backend.tournament.repository.TournamentEntryRepository;
import com.sentimospadel.backend.tournament.repository.TournamentMatchRepository;
import com.sentimospadel.backend.tournament.repository.TournamentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private static final int DEFAULT_LEAGUE_ROUNDS = 2;
    private static final int ELIMINATION_MIN_TEAMS = 4;
    private static final int AMERICANO_FIXED_MIN_TEAMS = 4;

    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchRepository tournamentMatchRepository;
    private final ClubRepository clubRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerProfileResolverService playerProfileResolverService;
    private final TournamentMapper tournamentMapper;

    @Transactional
    public TournamentResponse createTournament(String email, CreateTournamentRequest request) {
        validateTournamentDates(request.startDate(), request.endDate());

        PlayerProfile creator = playerProfileResolverService.getOrCreateByUserEmail(email);
        Club club = resolveClub(request.clubId());
        TournamentFormat format = request.format();

        validateAmericanoSettings(format, request.americanoType(), request.matchesPerParticipant());

        Tournament tournament = tournamentRepository.save(Tournament.builder()
                .createdBy(creator)
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .club(club)
                .city(trimToNull(request.city()))
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(TournamentStatus.OPEN)
                .format(format)
                .americanoType(request.americanoType())
                .maxEntries(request.maxEntries())
                .openEnrollment(request.openEnrollment() == null || request.openEnrollment())
                .competitive(request.competitive() == null || request.competitive())
                .leagueRounds(resolveLeagueRounds(format, request.leagueRounds()))
                .matchesPerParticipant(resolveMatchesPerParticipant(format, request.matchesPerParticipant()))
                .pointsForWin(pointsForWin(format))
                .pointsForTiebreakLoss(pointsForTiebreakLoss(format))
                .pointsForLoss(pointsForLoss(format))
                .standingsTiebreak(Optional.ofNullable(request.standingsTiebreak()).orElse(TournamentStandingsTiebreak.GAMES_DIFFERENCE))
                .availableCourts(request.availableCourts())
                .courtNames(normalizeStringList(request.courtNames()))
                .build());

        List<TournamentEntry> persistedEntries = persistEntries(tournament, creator, request.entries(), true);
        return toTournamentResponse(tournament, persistedEntries);
    }

    @Transactional
    public TournamentResponse joinTournament(String email, Long tournamentId) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureTournamentOpenForEntries(tournament);
        if (!tournament.isOpenEnrollment()) {
            throw new ConflictException("This tournament is closed for self-registration");
        }
        ensurePlayerIsNotAlreadyRegistered(tournamentId, playerProfile.getId());

        List<TournamentEntry> currentEntries = getEntries(tournamentId);
        validateEntryCapacity(tournament, currentEntries.size() + 1);

        TournamentEntry entry = TournamentEntry.builder()
                .tournament(tournament)
                .primaryPlayerProfile(playerProfile)
                .createdBy(playerProfile)
                .teamName(null)
                .entryKind(TournamentEntryKind.REGISTERED)
                .status(entryStatusForMembers(1, teamSizeFor(tournament)))
                .timePreferences(List.of())
                .build();

        tournamentEntryRepository.save(entry);
        return toTournamentResponse(tournament, getEntries(tournamentId));
    }

    @Transactional
    public TournamentResponse leaveTournament(String email, Long tournamentId) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureTournamentOpenForEntries(tournament);

        TournamentEntry entry = findEntryByPlayer(tournamentId, playerProfile.getId())
                .orElseThrow(() -> new ConflictException("Player is not registered for this tournament"));

        if (entry.getSecondaryPlayerProfile() != null && Objects.equals(entry.getSecondaryPlayerProfile().getId(), playerProfile.getId())) {
            entry.setSecondaryPlayerProfile(null);
            entry.setStatus(entryStatusForMembers(1, teamSizeFor(tournament)));
            entry.setTeamName(trimToNull(entry.getTeamName()));
            tournamentEntryRepository.save(entry);
        } else if (entry.getPrimaryPlayerProfile() != null && Objects.equals(entry.getPrimaryPlayerProfile().getId(), playerProfile.getId())) {
            if (entry.getSecondaryPlayerProfile() == null) {
                tournamentEntryRepository.delete(entry);
            } else {
                entry.setPrimaryPlayerProfile(entry.getSecondaryPlayerProfile());
                entry.setSecondaryPlayerProfile(null);
                entry.setStatus(entryStatusForMembers(1, teamSizeFor(tournament)));
                tournamentEntryRepository.save(entry);
            }
        }

        return toTournamentResponse(tournament, getEntries(tournamentId));
    }

    @Transactional
    public TournamentResponse syncEntries(String email, Long tournamentId, SyncTournamentEntriesRequest request) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile actor = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureCreator(tournament, actor);
        ensureTournamentOpenForEntries(tournament);

        List<TournamentEntry> persistedEntries = persistEntries(tournament, actor, request.entries(), false);
        return toTournamentResponse(tournament, persistedEntries);
    }

    @Transactional
    public TournamentResponse launchTournament(String email, Long tournamentId, LaunchTournamentRequest request) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile actor = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureCreator(tournament, actor);

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new ConflictException("Only OPEN tournaments can be launched");
        }

        if (tournamentMatchRepository.existsByTournamentId(tournamentId)) {
            throw new ConflictException("This tournament was already launched");
        }

        List<TournamentEntry> entries = getEntries(tournamentId).stream()
                .filter(entry -> entry.getStatus() == TournamentEntryStatus.CONFIRMED)
                .toList();

        switch (tournament.getFormat()) {
            case LEAGUE -> launchLeagueTournament(tournament, request, entries);
            case ELIMINATION -> launchEliminationTournament(tournament, request, entries);
            case AMERICANO -> {
                if (tournament.getAmericanoType() == TournamentAmericanoType.FIXED) {
                    launchFixedAmericanoTournament(tournament, request, entries);
                } else if (tournament.getAmericanoType() == TournamentAmericanoType.DYNAMIC) {
                    launchDynamicAmericanoTournament(tournament, request, entries, actor);
                } else {
                    throw new ConflictException("Unsupported AMERICANO launch configuration");
                }
            }
        }

        return toTournamentResponse(tournament, getEntries(tournamentId));
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getTournaments() {
        return tournamentRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Tournament::getStartDate).thenComparing(Tournament::getCreatedAt))
                .map(tournament -> toTournamentResponse(tournament, getEntries(tournament.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournamentById(Long tournamentId) {
        Tournament tournament = getTournamentEntity(tournamentId);
        return toTournamentResponse(tournament, getEntries(tournamentId));
    }

    @Transactional(readOnly = true)
    public Tournament getTournamentEntity(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament with id " + tournamentId + " was not found"));
    }

    @Transactional(readOnly = true)
    public List<TournamentEntry> getEntries(Long tournamentId) {
        return tournamentEntryRepository.findAllByTournamentIdAndEntryKindOrderByCreatedAtAsc(
                tournamentId,
                TournamentEntryKind.REGISTERED
        );
    }

    private TournamentResponse toTournamentResponse(Tournament tournament, List<TournamentEntry> entries) {
        return tournamentMapper.toTournamentResponse(tournament, entries, tournamentMatchRepository.countByTournamentId(tournament.getId()));
    }

    private void launchLeagueTournament(
            Tournament tournament,
            LaunchTournamentRequest request,
            List<TournamentEntry> entries
    ) {
        if (entries.size() < 2) {
            throw new ConflictException("A league tournament needs at least two confirmed teams before launch");
        }

        int availableCourts = resolveAvailableCourts(tournament, request.availableCourts());
        int numberOfGroups = request.numberOfGroups() == null ? 1 : request.numberOfGroups();
        if (numberOfGroups != 1) {
            throw new ConflictException("League MVP currently supports a single standings table only");
        }

        int leagueRounds = resolveLeagueRounds(TournamentFormat.LEAGUE, request.leagueRounds());
        List<String> courtNames = resolveCourtNames(tournament, request.courtNames(), availableCourts);
        validateScheduleCapacity(tournament, entries.size(), leagueRounds, availableCourts);

        tournament.setAvailableCourts(availableCourts);
        tournament.setNumberOfGroups(numberOfGroups);
        tournament.setLeagueRounds(leagueRounds);
        tournament.setCourtNames(courtNames);
        tournament.setOpenEnrollment(false);
        tournament.setLaunchedAt(Instant.now());
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);

        List<TournamentMatch> generatedMatches = generateLeagueMatches(tournament, entries, availableCourts, courtNames, leagueRounds);
        tournamentMatchRepository.saveAll(generatedMatches);
    }

    private void launchEliminationTournament(
            Tournament tournament,
            LaunchTournamentRequest request,
            List<TournamentEntry> entries
    ) {
        if (entries.size() < ELIMINATION_MIN_TEAMS) {
            throw new ConflictException("An elimination tournament needs at least four confirmed teams before launch");
        }

        int availableCourts = resolveAvailableCourts(tournament, request.availableCourts());
        int numberOfGroups = resolveEliminationGroups(request.numberOfGroups(), entries.size());
        List<String> courtNames = resolveCourtNames(tournament, request.courtNames(), availableCourts);

        tournament.setAvailableCourts(availableCourts);
        tournament.setNumberOfGroups(numberOfGroups);
        tournament.setLeagueRounds(1);
        tournament.setCourtNames(courtNames);
        tournament.setOpenEnrollment(false);
        tournament.setLaunchedAt(Instant.now());
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);

        List<TournamentEntry> groupedEntries = assignEliminationGroups(entries, numberOfGroups);
        tournamentEntryRepository.saveAll(groupedEntries);

        List<TournamentMatch> generatedMatches = generateEliminationGroupStageMatches(tournament, groupedEntries, availableCourts, courtNames);
        tournamentMatchRepository.saveAll(generatedMatches);
    }

    private void launchFixedAmericanoTournament(
            Tournament tournament,
            LaunchTournamentRequest request,
            List<TournamentEntry> entries
    ) {
        if (entries.size() < AMERICANO_FIXED_MIN_TEAMS) {
            throw new ConflictException("An Americano fijo tournament needs at least four confirmed teams before launch");
        }
        if (entries.size() % 2 != 0) {
            throw new ConflictException("Americano fijo currently requires an even number of confirmed teams");
        }

        int matchesPerParticipant = Optional.ofNullable(tournament.getMatchesPerParticipant())
                .filter(value -> value > 0)
                .orElseThrow(() -> new ConflictException("AMERICANO fijo requires matchesPerParticipant before launch"));
        if (matchesPerParticipant > entries.size() - 1) {
            throw new ConflictException("AMERICANO fijo currently supports at most one match against each opponent");
        }

        int availableCourts = resolveAvailableCourts(tournament, request.availableCourts());
        List<String> courtNames = resolveCourtNames(tournament, request.courtNames(), availableCourts);
        validateAmericanoFixedScheduleCapacity(tournament, entries.size(), matchesPerParticipant, availableCourts);

        tournament.setAvailableCourts(availableCourts);
        tournament.setNumberOfGroups(1);
        tournament.setLeagueRounds(1);
        tournament.setCourtNames(courtNames);
        tournament.setOpenEnrollment(false);
        tournament.setLaunchedAt(Instant.now());
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);

        List<TournamentMatch> generatedMatches = generateAmericanoFixedMatches(
                tournament,
                entries,
                availableCourts,
                courtNames,
                matchesPerParticipant
        );
        tournamentMatchRepository.saveAll(generatedMatches);
    }

    private void launchDynamicAmericanoTournament(
            Tournament tournament,
            LaunchTournamentRequest request,
            List<TournamentEntry> entries,
            PlayerProfile actor
    ) {
        if (entries.size() < AMERICANO_FIXED_MIN_TEAMS) {
            throw new ConflictException("An Americano dinámico tournament needs at least four confirmed players before launch");
        }

        int matchesPerParticipant = Optional.ofNullable(tournament.getMatchesPerParticipant())
                .filter(value -> value > 0)
                .orElseThrow(() -> new ConflictException("AMERICANO dinámico requires matchesPerParticipant before launch"));

        int availableCourts = resolveAvailableCourts(tournament, request.availableCourts());
        List<String> courtNames = resolveCourtNames(tournament, request.courtNames(), availableCourts);

        tournament.setAvailableCourts(availableCourts);
        tournament.setNumberOfGroups(1);
        tournament.setLeagueRounds(1);
        tournament.setCourtNames(courtNames);
        tournament.setOpenEnrollment(false);
        tournament.setLaunchedAt(Instant.now());
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);

        List<AmericanoDynamicMatchup> generatedMatchups = generateDynamicAmericanoMatchups(entries, matchesPerParticipant);
        validateGeneratedMatchCapacity(
                tournament,
                generatedMatchups.size(),
                availableCourts,
                "Tournament window does not have enough simple day/court slots for Americano dinámico launch"
        );
        List<TournamentEntry> generatedPairEntries = new ArrayList<>();
        List<GeneratedPairing> generatedPairings = new ArrayList<>();

        for (int matchIndex = 0; matchIndex < generatedMatchups.size(); matchIndex++) {
            AmericanoDynamicMatchup matchup = generatedMatchups.get(matchIndex);
            TournamentEntry teamOneEntry = buildGeneratedMatchPairEntry(
                    tournament,
                    actor,
                    matchup.teamOnePlayerOne(),
                    matchup.teamOnePlayerTwo()
            );
            TournamentEntry teamTwoEntry = buildGeneratedMatchPairEntry(
                    tournament,
                    actor,
                    matchup.teamTwoPlayerOne(),
                    matchup.teamTwoPlayerTwo()
            );
            generatedPairEntries.add(teamOneEntry);
            generatedPairEntries.add(teamTwoEntry);
            generatedPairings.add(new GeneratedPairing(matchup, teamOneEntry, teamTwoEntry));
        }

        List<TournamentEntry> savedPairEntries = tournamentEntryRepository.saveAll(generatedPairEntries);
        List<TournamentMatch> generatedMatches = new ArrayList<>();
        for (int matchIndex = 0; matchIndex < generatedPairings.size(); matchIndex++) {
            GeneratedPairing pairing = generatedPairings.get(matchIndex);
            TournamentEntry savedTeamOne = savedPairEntries.get(matchIndex * 2);
            TournamentEntry savedTeamTwo = savedPairEntries.get(matchIndex * 2 + 1);
            AmericanoDynamicMatchup matchup = pairing.matchup();
            generatedMatches.add(buildTournamentMatch(
                    tournament,
                    savedTeamOne,
                    savedTeamTwo,
                    TournamentMatchPhase.AMERICANO_STAGE,
                    matchup.roundNumber(),
                    null,
                    "Ronda " + matchup.roundNumber(),
                    availableCourts,
                    courtNames,
                    matchIndex
            ));
        }

        tournamentMatchRepository.saveAll(generatedMatches);
    }

    private List<TournamentEntry> persistEntries(
            Tournament tournament,
            PlayerProfile actor,
            List<TournamentEntryUpsertRequest> requests,
            boolean creatingTournament
    ) {
        List<TournamentEntryUpsertRequest> normalizedRequests = requests == null ? List.of() : requests;
        validateEntryCapacity(tournament, normalizedRequests.size());
        validateEntryRequests(tournament, normalizedRequests);

        if (!creatingTournament) {
            tournamentEntryRepository.deleteAll(
                    tournamentEntryRepository.findAllByTournamentIdAndEntryKind(
                            tournament.getId(),
                            TournamentEntryKind.REGISTERED
                    )
            );
        }

        if (normalizedRequests.isEmpty()) {
            return getEntries(tournament.getId());
        }

        Map<Long, PlayerProfile> playerById = loadPlayers(normalizedRequests);
        List<TournamentEntry> entries = new ArrayList<>();

        for (TournamentEntryUpsertRequest request : normalizedRequests) {
            List<Long> memberIds = request.members().stream()
                    .map(member -> member.playerProfileId())
                    .toList();

            TournamentEntry entry = TournamentEntry.builder()
                    .tournament(tournament)
                    .primaryPlayerProfile(playerById.get(memberIds.getFirst()))
                    .secondaryPlayerProfile(memberIds.size() > 1 ? playerById.get(memberIds.get(1)) : null)
                    .createdBy(actor)
                    .teamName(trimToNull(request.teamName()))
                    .entryKind(TournamentEntryKind.REGISTERED)
                    .status(entryStatusForMembers(memberIds.size(), teamSizeFor(tournament)))
                    .timePreferences(normalizeStringList(request.timePreferences()))
                    .build();
            entries.add(entry);
        }

        return tournamentEntryRepository.saveAll(entries);
    }

    private Map<Long, PlayerProfile> loadPlayers(List<TournamentEntryUpsertRequest> requests) {
        Set<Long> playerIds = new HashSet<>();
        requests.forEach(request -> request.members().forEach(member -> playerIds.add(member.playerProfileId())));

        Map<Long, PlayerProfile> playerById = new HashMap<>();
        playerProfileRepository.findAllById(playerIds).forEach(player -> playerById.put(player.getId(), player));

        if (playerById.size() != playerIds.size()) {
            throw new ResourceNotFoundException("One or more player profiles in the tournament entry payload were not found");
        }

        return playerById;
    }

    private void validateEntryRequests(Tournament tournament, List<TournamentEntryUpsertRequest> requests) {
        int teamSize = teamSizeFor(tournament);
        Set<Long> seenPlayers = new HashSet<>();

        for (TournamentEntryUpsertRequest request : requests) {
            List<Long> memberIds = request.members().stream().map(member -> member.playerProfileId()).toList();
            if (memberIds.size() > teamSize) {
                throw new BadRequestException("Tournament entries exceed the supported team size for this format");
            }
            if (memberIds.isEmpty()) {
                throw new BadRequestException("Tournament entries must contain at least one player");
            }
            if (new HashSet<>(memberIds).size() != memberIds.size()) {
                throw new BadRequestException("Tournament entries cannot repeat the same player inside a team");
            }
            for (Long playerId : memberIds) {
                if (!seenPlayers.add(playerId)) {
                    throw new ConflictException("The same player cannot appear in multiple tournament entries");
                }
            }
        }
    }

    private Optional<TournamentEntry> findEntryByPlayer(Long tournamentId, Long playerProfileId) {
        return getEntries(tournamentId).stream()
                .filter(entry -> Objects.equals(entry.getPrimaryPlayerProfile().getId(), playerProfileId)
                        || (entry.getSecondaryPlayerProfile() != null
                        && Objects.equals(entry.getSecondaryPlayerProfile().getId(), playerProfileId)))
                .findFirst();
    }

    private void ensurePlayerIsNotAlreadyRegistered(Long tournamentId, Long playerProfileId) {
        if (findEntryByPlayer(tournamentId, playerProfileId).isPresent()) {
            throw new ConflictException("Player is already registered for this tournament");
        }
    }

    private void ensureCreator(Tournament tournament, PlayerProfile actor) {
        if (!tournament.getCreatedBy().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the tournament creator can perform this action");
        }
    }

    private Club resolveClub(Long clubId) {
        if (clubId == null) {
            return null;
        }

        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club with id " + clubId + " was not found"));
    }

    private void validateTournamentDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("Tournament endDate cannot be before startDate");
        }
    }

    private void validateAmericanoSettings(
            TournamentFormat format,
            TournamentAmericanoType americanoType,
            Integer matchesPerParticipant
    ) {
        if (format != TournamentFormat.AMERICANO && americanoType != null) {
            throw new BadRequestException("americanoType can only be used when format is AMERICANO");
        }
        if (format != TournamentFormat.AMERICANO && matchesPerParticipant != null) {
            throw new BadRequestException("matchesPerParticipant can only be used when format is AMERICANO");
        }
        if (format == TournamentFormat.AMERICANO && americanoType == null) {
            throw new BadRequestException("AMERICANO tournaments require americanoType");
        }
        if (format == TournamentFormat.AMERICANO && (matchesPerParticipant == null || matchesPerParticipant <= 0)) {
            throw new BadRequestException("AMERICANO tournaments require a positive matchesPerParticipant");
        }
    }

    private int resolveLeagueRounds(TournamentFormat format, Integer requestedRounds) {
        if (format != TournamentFormat.LEAGUE) {
            return requestedRounds == null ? 1 : requestedRounds;
        }

        int resolved = requestedRounds == null ? DEFAULT_LEAGUE_ROUNDS : requestedRounds;
        if (resolved != 2) {
            throw new BadRequestException("League MVP currently supports double round-robin only");
        }
        return resolved;
    }

    private Integer resolveMatchesPerParticipant(TournamentFormat format, Integer requestedMatchesPerParticipant) {
        if (format != TournamentFormat.AMERICANO) {
            return null;
        }
        if (requestedMatchesPerParticipant == null || requestedMatchesPerParticipant <= 0) {
            throw new BadRequestException("AMERICANO tournaments require a positive matchesPerParticipant");
        }
        return requestedMatchesPerParticipant;
    }

    private void ensureTournamentOpenForEntries(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new ConflictException("Tournament entries are only available while the tournament is OPEN");
        }
    }

    private int teamSizeFor(Tournament tournament) {
        if (tournament.getFormat() == TournamentFormat.AMERICANO
                && tournament.getAmericanoType() == TournamentAmericanoType.DYNAMIC) {
            return 1;
        }
        return 2;
    }

    private TournamentEntryStatus entryStatusForMembers(int memberCount, int teamSize) {
        return memberCount >= teamSize ? TournamentEntryStatus.CONFIRMED : TournamentEntryStatus.PENDING;
    }

    private void validateEntryCapacity(Tournament tournament, int entryCount) {
        if (tournament.getMaxEntries() != null && entryCount > tournament.getMaxEntries()) {
            throw new ConflictException("Tournament has reached its maximum number of entries");
        }
    }

    private int resolveAvailableCourts(Tournament tournament, Integer requestedAvailableCourts) {
        Integer resolved = requestedAvailableCourts != null ? requestedAvailableCourts : tournament.getAvailableCourts();
        if (resolved == null || resolved <= 0) {
            throw new BadRequestException("availableCourts must be provided before launching the tournament");
        }
        return resolved;
    }

    private List<String> resolveCourtNames(Tournament tournament, List<String> requestedCourtNames, int availableCourts) {
        List<String> provided = requestedCourtNames != null ? requestedCourtNames : tournament.getCourtNames();
        if (provided == null || provided.isEmpty()) {
            List<String> generated = new ArrayList<>();
            for (int index = 1; index <= availableCourts; index++) {
                generated.add("Cancha " + index);
            }
            return generated;
        }
        return normalizeStringList(provided).stream().limit(availableCourts).toList();
    }

    private void validateScheduleCapacity(Tournament tournament, int confirmedTeamsCount, int leagueRounds, int availableCourts) {
        int fixtures = roundRobinMatchCount(confirmedTeamsCount) * leagueRounds;
        LocalDate endDate = tournament.getEndDate() == null ? tournament.getStartDate() : tournament.getEndDate();
        long availableDays = tournament.getStartDate().datesUntil(endDate.plusDays(1)).count();
        long availableSlots = availableDays * availableCourts;

        if (fixtures > availableSlots) {
            throw new ConflictException("Tournament window does not have enough simple day/court slots for league launch");
        }
    }

    private int roundRobinMatchCount(int teams) {
        return teams * (teams - 1) / 2;
    }

    private int pointsForWin(TournamentFormat format) {
        if (format == TournamentFormat.ELIMINATION) {
            return 2;
        }
        if (format == TournamentFormat.AMERICANO) {
            return 2;
        }
        return 3;
    }

    private int pointsForTiebreakLoss(TournamentFormat format) {
        if (format == TournamentFormat.ELIMINATION || format == TournamentFormat.AMERICANO) {
            return 0;
        }
        return 1;
    }

    private int pointsForLoss(TournamentFormat format) {
        return 0;
    }

    private List<TournamentMatch> generateLeagueMatches(
            Tournament tournament,
            List<TournamentEntry> confirmedEntries,
            int availableCourts,
            List<String> courtNames,
            int leagueRounds
    ) {
        List<TournamentEntry> rotation = new ArrayList<>(confirmedEntries);
        boolean hasBye = rotation.size() % 2 != 0;
        if (hasBye) {
            rotation.add(null);
        }

        int teams = rotation.size();
        int roundsPerLeg = teams - 1;
        int matchesPerRound = teams / 2;
        List<TournamentMatch> matches = new ArrayList<>();
        List<TournamentEntry> current = new ArrayList<>(rotation);

        int slotIndex = 0;
        for (int leg = 1; leg <= leagueRounds; leg++) {
            for (int round = 0; round < roundsPerLeg; round++) {
                for (int pair = 0; pair < matchesPerRound; pair++) {
                    TournamentEntry teamOne = current.get(pair);
                    TournamentEntry teamTwo = current.get(teams - 1 - pair);
                    if (teamOne == null || teamTwo == null) {
                        continue;
                    }

                    TournamentEntry actualTeamOne = leg % 2 == 0 ? teamTwo : teamOne;
                    TournamentEntry actualTeamTwo = leg % 2 == 0 ? teamOne : teamTwo;
                    matches.add(buildTournamentMatch(
                            tournament,
                            actualTeamOne,
                            actualTeamTwo,
                            TournamentMatchPhase.LEAGUE_STAGE,
                            round + 1,
                            leg,
                            "Jornada " + (round + 1) + " - Vuelta " + leg,
                            availableCourts,
                            courtNames,
                            slotIndex++
                    ));
                }
                current = rotateRoundRobin(current);
            }
        }
        return matches;
    }

    private int resolveEliminationGroups(Integer requestedGroups, int confirmedTeamsCount) {
        int resolved = requestedGroups == null ? 1 : requestedGroups;
        if (resolved <= 0) {
            throw new BadRequestException("numberOfGroups must be positive");
        }
        return Math.min(resolved, confirmedTeamsCount);
    }

    private List<TournamentEntry> assignEliminationGroups(List<TournamentEntry> entries, int numberOfGroups) {
        List<TournamentEntry> assignedEntries = new ArrayList<>(entries);
        for (int index = 0; index < assignedEntries.size(); index++) {
            TournamentEntry entry = assignedEntries.get(index);
            char groupLetter = (char) ('A' + (index % numberOfGroups));
            entry.setGroupLabel("Grupo " + groupLetter);
        }
        return assignedEntries;
    }

    private List<TournamentMatch> generateEliminationGroupStageMatches(
            Tournament tournament,
            List<TournamentEntry> groupedEntries,
            int availableCourts,
            List<String> courtNames
    ) {
        Map<String, List<TournamentEntry>> entriesByGroup = new LinkedHashMap<>();
        groupedEntries.forEach(entry -> entriesByGroup.computeIfAbsent(entry.getGroupLabel(), ignored -> new ArrayList<>()).add(entry));

        List<TournamentMatch> matches = new ArrayList<>();
        int slotIndex = 0;
        int roundNumber = 1;

        for (Map.Entry<String, List<TournamentEntry>> group : entriesByGroup.entrySet()) {
            List<TournamentEntry> groupEntries = group.getValue();
            for (int leftIndex = 0; leftIndex < groupEntries.size(); leftIndex++) {
                for (int rightIndex = leftIndex + 1; rightIndex < groupEntries.size(); rightIndex++) {
                    matches.add(buildTournamentMatch(
                            tournament,
                            groupEntries.get(leftIndex),
                            groupEntries.get(rightIndex),
                            TournamentMatchPhase.GROUP_STAGE,
                            roundNumber++,
                            null,
                            "Fase de Grupos - " + group.getKey(),
                            availableCourts,
                            courtNames,
                            slotIndex++
                    ));
                }
            }
        }

        return matches;
    }

    private void validateAmericanoFixedScheduleCapacity(
            Tournament tournament,
            int confirmedTeamsCount,
            int matchesPerParticipant,
            int availableCourts
    ) {
        int fixtures = (confirmedTeamsCount * matchesPerParticipant) / 2;
        LocalDate endDate = tournament.getEndDate() == null ? tournament.getStartDate() : tournament.getEndDate();
        long availableDays = tournament.getStartDate().datesUntil(endDate.plusDays(1)).count();
        long availableSlots = availableDays * availableCourts;

        if (fixtures > availableSlots) {
            throw new ConflictException("Tournament window does not have enough simple day/court slots for Americano fijo launch");
        }
    }

    private void validateGeneratedMatchCapacity(
            Tournament tournament,
            int fixtures,
            int availableCourts,
            String errorMessage
    ) {
        LocalDate endDate = tournament.getEndDate() == null ? tournament.getStartDate() : tournament.getEndDate();
        long availableDays = tournament.getStartDate().datesUntil(endDate.plusDays(1)).count();
        long availableSlots = availableDays * availableCourts;

        if (fixtures > availableSlots) {
            throw new ConflictException(errorMessage);
        }
    }

    private List<TournamentMatch> generateAmericanoFixedMatches(
            Tournament tournament,
            List<TournamentEntry> confirmedEntries,
            int availableCourts,
            List<String> courtNames,
            int matchesPerParticipant
    ) {
        List<TournamentEntry> current = new ArrayList<>(confirmedEntries);
        List<TournamentMatch> matches = new ArrayList<>();

        int teams = current.size();
        int matchesPerRound = teams / 2;
        int slotIndex = 0;

        for (int round = 0; round < matchesPerParticipant; round++) {
            for (int pair = 0; pair < matchesPerRound; pair++) {
                TournamentEntry teamOne = current.get(pair);
                TournamentEntry teamTwo = current.get(teams - 1 - pair);
                matches.add(buildTournamentMatch(
                        tournament,
                        teamOne,
                        teamTwo,
                        TournamentMatchPhase.AMERICANO_STAGE,
                        round + 1,
                        null,
                        "Ronda " + (round + 1),
                        availableCourts,
                        courtNames,
                        slotIndex++
                ));
            }
            current = rotateRoundRobin(current);
        }

        return matches;
    }

    private List<AmericanoDynamicMatchup> generateDynamicAmericanoMatchups(
            List<TournamentEntry> playerEntries,
            int matchesPerParticipant
    ) {
        List<PlayerProfile> players = playerEntries.stream()
                .map(TournamentEntry::getPrimaryPlayerProfile)
                .filter(Objects::nonNull)
                .toList();

        if (players.size() < 4) {
            throw new ConflictException("AMERICANO dinámico needs at least four players");
        }

        Map<Long, Integer> matchCount = new HashMap<>();
        Map<Long, Set<Long>> partnerHistory = new HashMap<>();
        players.forEach(player -> {
            matchCount.put(player.getId(), 0);
            partnerHistory.put(player.getId(), new HashSet<>());
        });

        List<AmericanoDynamicMatchup> generatedMatchups = new ArrayList<>();
        int matchesPerRound = Math.max(1, players.size() / 4);

        while (matchCount.values().stream().anyMatch(count -> count < matchesPerParticipant)) {
            List<PlayerProfile> sortedPlayers = new ArrayList<>(players);
            sortedPlayers.sort(Comparator
                    .comparing((PlayerProfile player) -> (matchCount.get(player.getId()) < matchesPerParticipant) ? 0 : 1)
                    .thenComparing(player -> matchCount.get(player.getId()))
                    .thenComparing(player -> partnerHistory.get(player.getId()).size()));

            PlayerProfile p1 = sortedPlayers.getFirst();
            PlayerProfile p2 = findPreferredPartner(sortedPlayers.subList(1, sortedPlayers.size()), p1, matchCount, partnerHistory, matchesPerParticipant);
            if (p2 == null) {
                throw new ConflictException("AMERICANO dinámico could not generate a valid partner for one player");
            }

            List<PlayerProfile> selected = new ArrayList<>(List.of(p1, p2));
            List<PlayerProfile> remaining = sortedPlayers.stream()
                    .filter(player -> selected.stream().noneMatch(chosen -> chosen.getId().equals(player.getId())))
                    .toList();
            if (remaining.size() < 2) {
                throw new ConflictException("AMERICANO dinámico could not complete a 4-player match lineup");
            }

            PlayerProfile p3 = remaining.getFirst();
            PlayerProfile p4 = findPreferredPartner(remaining.subList(1, remaining.size()), p3, matchCount, partnerHistory, matchesPerParticipant);
            if (p4 == null) {
                throw new ConflictException("AMERICANO dinámico could not generate an opposing partner");
            }

            List<PlayerProfile> selectedForMatch = List.of(p1, p2, p3, p4);
            selectedForMatch.forEach(player -> matchCount.compute(player.getId(), (ignored, current) -> current == null ? 1 : current + 1));
            partnerHistory.get(p1.getId()).add(p2.getId());
            partnerHistory.get(p2.getId()).add(p1.getId());
            partnerHistory.get(p3.getId()).add(p4.getId());
            partnerHistory.get(p4.getId()).add(p3.getId());

            generatedMatchups.add(new AmericanoDynamicMatchup(
                    p1,
                    p2,
                    p3,
                    p4,
                    (generatedMatchups.size() / matchesPerRound) + 1
            ));
        }

        return generatedMatchups;
    }

    private PlayerProfile findPreferredPartner(
            List<PlayerProfile> candidates,
            PlayerProfile anchor,
            Map<Long, Integer> matchCount,
            Map<Long, Set<Long>> partnerHistory,
            int matchesPerParticipant
    ) {
        return candidates.stream()
                .filter(candidate -> matchCount.get(candidate.getId()) < matchesPerParticipant
                        && !partnerHistory.get(anchor.getId()).contains(candidate.getId()))
                .findFirst()
                .or(() -> candidates.stream()
                        .filter(candidate -> matchCount.get(candidate.getId()) < matchesPerParticipant)
                        .findFirst())
                .orElse(candidates.isEmpty() ? null : candidates.getFirst());
    }

    private TournamentEntry buildGeneratedMatchPairEntry(
            Tournament tournament,
            PlayerProfile actor,
            PlayerProfile primary,
            PlayerProfile secondary
    ) {
        return TournamentEntry.builder()
                .tournament(tournament)
                .primaryPlayerProfile(primary)
                .secondaryPlayerProfile(secondary)
                .createdBy(actor)
                .teamName(displayGeneratedDynamicPairName(primary, secondary))
                .entryKind(TournamentEntryKind.GENERATED_MATCH_PAIR)
                .status(TournamentEntryStatus.CONFIRMED)
                .timePreferences(List.of())
                .build();
    }

    private String displayGeneratedDynamicPairName(PlayerProfile primary, PlayerProfile secondary) {
        return firstName(primary.getFullName()) + " & " + firstName(secondary.getFullName());
    }

    private String firstName(String fullName) {
        return fullName == null || fullName.isBlank()
                ? "Jugador"
                : fullName.trim().split("\\s+")[0];
    }

    private TournamentMatch buildTournamentMatch(
            Tournament tournament,
            TournamentEntry teamOne,
            TournamentEntry teamTwo,
            TournamentMatchPhase phase,
            int roundNumber,
            Integer legNumber,
            String roundLabel,
            int availableCourts,
            List<String> courtNames,
            int slotIndex
    ) {
        int courtIndex = slotIndex % availableCourts;
        LocalDate matchDate = tournament.getStartDate().plusDays(slotIndex / availableCourts);
        Instant scheduledAt = matchDate.atTime(18, 0).toInstant(ZoneOffset.UTC);

        return TournamentMatch.builder()
                .tournament(tournament)
                .teamOneEntry(teamOne)
                .teamTwoEntry(teamTwo)
                .phase(phase)
                .status(TournamentMatchStatus.SCHEDULED)
                .roundNumber(roundNumber)
                .legNumber(legNumber)
                .roundLabel(roundLabel)
                .scheduledAt(scheduledAt)
                .courtName(courtNames.get(Math.min(courtIndex, courtNames.size() - 1)))
                .build();
    }

    private List<TournamentEntry> rotateRoundRobin(List<TournamentEntry> entries) {
        if (entries.size() <= 2) {
            return entries;
        }

        List<TournamentEntry> rotated = new ArrayList<>();
        rotated.add(entries.getFirst());
        rotated.add(entries.getLast());
        rotated.addAll(entries.subList(1, entries.size() - 1));
        return rotated;
    }

    private List<String> normalizeStringList(Collection<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record AmericanoDynamicMatchup(
            PlayerProfile teamOnePlayerOne,
            PlayerProfile teamOnePlayerTwo,
            PlayerProfile teamTwoPlayerOne,
            PlayerProfile teamTwoPlayerTwo,
            int roundNumber
    ) {
    }

    private record GeneratedPairing(
            AmericanoDynamicMatchup matchup,
            TournamentEntry teamOneEntry,
            TournamentEntry teamTwoEntry
    ) {
    }
}
