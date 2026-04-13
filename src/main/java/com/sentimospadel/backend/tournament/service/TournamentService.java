package com.sentimospadel.backend.tournament.service;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.notification.service.PlayerEventNotificationService;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.tournament.dto.CreateTournamentRequest;
import com.sentimospadel.backend.tournament.dto.LaunchTournamentRequest;
import com.sentimospadel.backend.tournament.dto.UpdateTournamentEntryTeamNameRequest;
import com.sentimospadel.backend.tournament.dto.SyncTournamentEntriesRequest;
import com.sentimospadel.backend.tournament.dto.TournamentLaunchPreviewGroupResponse;
import com.sentimospadel.backend.tournament.dto.TournamentLaunchPreviewMatchResponse;
import com.sentimospadel.backend.tournament.dto.TournamentLaunchPreviewResponse;
import com.sentimospadel.backend.tournament.dto.TournamentLaunchPreviewTeamResponse;
import com.sentimospadel.backend.tournament.dto.TournamentEntryUpsertRequest;
import com.sentimospadel.backend.tournament.dto.TournamentResponse;
import com.sentimospadel.backend.tournament.dto.UpsertMyTournamentEntryRequest;
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
import java.time.LocalTime;
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
    private final PlayerEventNotificationService playerEventNotificationService;

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
                .categoryLabels(normalizeStringList(request.categoryLabels()))
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
    public TournamentResponse joinTournament(String email, Long tournamentId, UpsertMyTournamentEntryRequest request) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile playerProfile = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureTournamentOpenForEntries(tournament);
        if (!tournament.isOpenEnrollment()) {
            throw new ConflictException("This tournament is closed for self-registration");
        }
        ensurePlayerIsNotAlreadyRegistered(tournamentId, playerProfile.getId());
        RegistrationDraft registrationDraft = buildRegistrationDraft(tournament, playerProfile, request, null);

        List<TournamentEntry> currentEntries = getEntries(tournamentId);
        validateEntryCapacity(tournament, currentEntries.size() + 1);

        TournamentEntry entry = TournamentEntry.builder()
                .tournament(tournament)
                .primaryPlayerProfile(registrationDraft.primaryPlayer())
                .secondaryPlayerProfile(registrationDraft.secondaryPlayer())
                .createdBy(playerProfile)
                .teamName(registrationDraft.teamName())
                .entryKind(TournamentEntryKind.REGISTERED)
                .status(entryStatusForMembers(registrationDraft.memberCount(), teamSizeFor(tournament)))
                .timePreferences(registrationDraft.timePreferences())
                .build();

        tournamentEntryRepository.save(entry);
        return toTournamentResponse(tournament, getEntries(tournamentId));
    }

    @Transactional
    public TournamentResponse updateMyEntry(String email, Long tournamentId, UpsertMyTournamentEntryRequest request) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile actor = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureTournamentOpenForEntries(tournament);

        TournamentEntry entry = findEntryByPlayer(tournamentId, actor.getId())
                .orElseThrow(() -> new ConflictException("Player is not registered for this tournament"));

        RegistrationDraft registrationDraft = buildRegistrationDraft(tournament, actor, request, entry);

        entry.setTeamName(registrationDraft.teamName());
        entry.setTimePreferences(registrationDraft.timePreferences());
        entry.setStatus(entryStatusForMembers(registrationDraft.memberCount(), teamSizeFor(tournament)));

        if (canActorChangeOwnPartner(entry, actor)) {
            entry.setSecondaryPlayerProfile(registrationDraft.secondaryPlayer());
        }

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
        List<TournamentEntry> entries = prepareLaunchEntries(tournament, actor);

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

        playerEventNotificationService.notifyTournamentLaunched(tournament, entries);

        return toTournamentResponse(tournament, getEntries(tournamentId));
    }

    @Transactional
    public TournamentLaunchPreviewResponse previewLaunchTournament(String email, Long tournamentId, LaunchTournamentRequest request) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile actor = playerProfileResolverService.getOrCreateByUserEmail(email);
        List<TournamentEntry> entries = prepareLaunchEntries(tournament, actor);

        return switch (tournament.getFormat()) {
            case LEAGUE -> previewLeagueLaunch(tournament, request, entries);
            case ELIMINATION -> previewEliminationLaunch(tournament, request, entries);
            case AMERICANO -> previewAmericanoLaunch(tournament, request, entries, actor);
        };
    }

    @Transactional
    public TournamentResponse archiveTournament(String email, Long tournamentId) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile actor = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureCreator(tournament, actor);

        if (tournament.isArchived()) {
            throw new ConflictException("This tournament is already archived");
        }

        tournament.setArchived(true);
        tournament.setArchivedAt(Instant.now());
        tournamentRepository.save(tournament);

        return toTournamentResponse(tournament, getEntries(tournamentId));
    }

    @Transactional
    public TournamentResponse updateMyEntryTeamName(String email, Long tournamentId, UpdateTournamentEntryTeamNameRequest request) {
        Tournament tournament = getTournamentEntity(tournamentId);
        PlayerProfile actor = playerProfileResolverService.getOrCreateByUserEmail(email);

        ensureTournamentOpenForEntries(tournament);

        TournamentEntry entry = findEntryByPlayer(tournamentId, actor.getId())
                .orElseThrow(() -> new ConflictException("Player is not registered for this tournament"));

        if (entry.getStatus() != TournamentEntryStatus.CONFIRMED) {
            throw new ConflictException("Team name can only be updated once the team is confirmed");
        }

        entry.setTeamName(request.teamName().trim());
        tournamentEntryRepository.save(entry);

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

    private List<TournamentEntry> prepareLaunchEntries(Tournament tournament, PlayerProfile actor) {
        ensureCreator(tournament, actor);

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new ConflictException("Only OPEN tournaments can be launched or previewed");
        }

        if (tournamentMatchRepository.existsByTournamentId(tournament.getId())) {
            throw new ConflictException("This tournament was already launched");
        }

        return getEntries(tournament.getId()).stream()
                .filter(entry -> entry.getStatus() == TournamentEntryStatus.CONFIRMED)
                .toList();
    }

    private TournamentResponse toTournamentResponse(Tournament tournament, List<TournamentEntry> entries) {
        return tournamentMapper.toTournamentResponse(tournament, entries, tournamentMatchRepository.countByTournamentId(tournament.getId()));
    }

    private TournamentLaunchPreviewResponse previewLeagueLaunch(
            Tournament tournament,
            LaunchTournamentRequest request,
            List<TournamentEntry> entries
    ) {
        if (entries.size() < 2) {
            throw new ConflictException("A league tournament needs at least two confirmed teams before launch");
        }

        int availableCourts = resolveAvailableCourts(tournament, request.availableCourts());
        int numberOfGroups = 1;
        int leagueRounds = resolveLeagueRounds(TournamentFormat.LEAGUE, request.leagueRounds());
        List<String> courtNames = resolveCourtNames(tournament, request.courtNames(), availableCourts);
        validateScheduleCapacity(tournament, entries.size(), leagueRounds, availableCourts);
        List<TournamentMatch> stageMatches = generateLeagueMatches(tournament, entries, availableCourts, courtNames, leagueRounds);

        return new TournamentLaunchPreviewResponse(
                availableCourts,
                numberOfGroups,
                leagueRounds,
                courtNames,
                List.of(buildPreviewGroup("Liga", entries)),
                stageMatches.stream().map(this::toPreviewMatchResponse).toList(),
                List.of()
        );
    }

    private TournamentLaunchPreviewResponse previewEliminationLaunch(
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
        List<TournamentEntry> groupedEntries = assignEliminationGroups(copyEntries(entries), numberOfGroups);
        List<TournamentMatch> stageMatches = generateEliminationGroupStageMatches(tournament, groupedEntries, availableCourts, courtNames);

        return new TournamentLaunchPreviewResponse(
                availableCourts,
                numberOfGroups,
                1,
                courtNames,
                buildPreviewGroups(groupedEntries, "Grupo A"),
                stageMatches.stream().map(this::toPreviewMatchResponse).toList(),
                buildEliminationPlayoffPreview(tournament, groupedEntries, stageMatches, availableCourts, courtNames)
        );
    }

    private TournamentLaunchPreviewResponse previewAmericanoLaunch(
            Tournament tournament,
            LaunchTournamentRequest request,
            List<TournamentEntry> entries,
            PlayerProfile actor
    ) {
        if (tournament.getAmericanoType() == TournamentAmericanoType.FIXED) {
            return previewFixedAmericanoLaunch(tournament, request, entries);
        }
        return previewDynamicAmericanoLaunch(tournament, request, entries, actor);
    }

    private TournamentLaunchPreviewResponse previewFixedAmericanoLaunch(
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
        List<TournamentMatch> stageMatches = generateAmericanoFixedMatches(
                tournament,
                entries,
                availableCourts,
                courtNames,
                matchesPerParticipant
        );

        return new TournamentLaunchPreviewResponse(
                availableCourts,
                1,
                1,
                courtNames,
                List.of(buildPreviewGroup("Americano fijo", entries)),
                stageMatches.stream().map(this::toPreviewMatchResponse).toList(),
                List.of()
        );
    }

    private TournamentLaunchPreviewResponse previewDynamicAmericanoLaunch(
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
        List<AmericanoDynamicMatchup> generatedMatchups = generateDynamicAmericanoMatchups(entries, matchesPerParticipant);
        validateGeneratedMatchCapacity(
                tournament,
                generatedMatchups.size(),
                availableCourts,
                "Tournament window does not have enough simple day/court slots for Americano dinámico launch"
        );

        List<TournamentMatch> stageMatches = new ArrayList<>();
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
            stageMatches.add(buildTournamentMatch(
                    tournament,
                    teamOneEntry,
                    teamTwoEntry,
                    TournamentMatchPhase.AMERICANO_STAGE,
                    matchup.roundNumber(),
                    null,
                    "Ronda " + matchup.roundNumber(),
                    availableCourts,
                    courtNames,
                    matchIndex
            ));
        }

        return new TournamentLaunchPreviewResponse(
                availableCourts,
                1,
                1,
                courtNames,
                List.of(buildPreviewGroup("Americano dinámico", entries)),
                stageMatches.stream().map(this::toPreviewMatchResponse).toList(),
                List.of()
        );
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

    private RegistrationDraft buildRegistrationDraft(
            Tournament tournament,
            PlayerProfile actor,
            UpsertMyTournamentEntryRequest request,
            TournamentEntry existingEntry
    ) {
        int teamSize = teamSizeFor(tournament);
        String teamName = trimToNull(request == null ? null : request.teamName());
        List<String> timePreferences = normalizeStringList(request == null ? null : request.timePreferences());

        if (teamSize == 1) {
            if (request != null && request.secondaryPlayerProfileId() != null) {
                throw new BadRequestException("This tournament format does not support adding a teammate");
            }
            return new RegistrationDraft(actor, null, teamName, timePreferences);
        }

        PlayerProfile currentSecondary = existingEntry == null ? null : existingEntry.getSecondaryPlayerProfile();
        PlayerProfile secondaryPlayer = resolveSecondaryPlayerForRegistration(
                tournament,
                actor,
                request == null ? null : request.secondaryPlayerProfileId(),
                existingEntry,
                currentSecondary
        );

        return new RegistrationDraft(actor, secondaryPlayer, teamName, timePreferences);
    }

    private PlayerProfile resolveSecondaryPlayerForRegistration(
            Tournament tournament,
            PlayerProfile actor,
            Long requestedSecondaryPlayerProfileId,
            TournamentEntry existingEntry,
            PlayerProfile currentSecondary
    ) {
        if (requestedSecondaryPlayerProfileId == null) {
            return existingEntry != null && !canActorChangeOwnPartner(existingEntry, actor)
                    ? currentSecondary
                    : null;
        }

        if (requestedSecondaryPlayerProfileId.equals(actor.getId())) {
            throw new BadRequestException("A tournament team cannot repeat the same player");
        }

        if (existingEntry != null && !canActorChangeOwnPartner(existingEntry, actor)) {
            if (currentSecondary != null && currentSecondary.getId().equals(requestedSecondaryPlayerProfileId)) {
                return currentSecondary;
            }
            throw new AccessDeniedException("Only the primary player who created the entry can change the teammate");
        }

        PlayerProfile secondaryPlayer = playerProfileRepository.findById(requestedSecondaryPlayerProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Player profile " + requestedSecondaryPlayerProfileId + " was not found"));

        Optional<TournamentEntry> existingTeamForSecondary = findEntryByPlayer(tournament.getId(), secondaryPlayer.getId());
        if (existingTeamForSecondary.isPresent()
                && (existingEntry == null || !existingTeamForSecondary.get().getId().equals(existingEntry.getId()))) {
            throw new ConflictException("The selected teammate is already registered for this tournament");
        }

        return secondaryPlayer;
    }

    private boolean canActorChangeOwnPartner(TournamentEntry entry, PlayerProfile actor) {
        return entry.getCreatedBy() != null
                && Objects.equals(entry.getCreatedBy().getId(), actor.getId())
                && entry.getPrimaryPlayerProfile() != null
                && Objects.equals(entry.getPrimaryPlayerProfile().getId(), actor.getId());
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
        long availableSlots = availableDays * availableCourts * TimeBand.values().length;

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
        List<MatchSeed> seeds = new ArrayList<>();
        List<TournamentEntry> current = new ArrayList<>(rotation);

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
                    seeds.add(new MatchSeed(
                            actualTeamOne,
                            actualTeamTwo,
                            TournamentMatchPhase.LEAGUE_STAGE,
                            round + 1,
                            leg,
                            "Jornada " + (round + 1) + " - Vuelta " + leg
                    ));
                }
                current = rotateRoundRobin(current);
            }
        }
        return scheduleMatchSeeds(tournament, seeds, availableCourts, courtNames);
    }

    private int resolveEliminationGroups(Integer requestedGroups, int confirmedTeamsCount) {
        int resolved = requestedGroups == null ? 1 : requestedGroups;
        if (resolved <= 0) {
            throw new BadRequestException("numberOfGroups must be positive");
        }
        if (resolved != 1 && resolved != 2 && resolved != 4 && resolved != 8) {
            throw new BadRequestException("Elimination MVP currently supports 1, 2, 4 or 8 groups only");
        }
        if (resolved > Math.max(1, confirmedTeamsCount / 2)) {
            throw new ConflictException("Elimination groups need at least two confirmed teams per group before launch");
        }
        return resolved;
    }

    private List<TournamentEntry> assignEliminationGroups(List<TournamentEntry> entries, int numberOfGroups) {
        List<TournamentEntry> assignedEntries = new ArrayList<>(entries);
        List<List<TournamentEntry>> groups = new ArrayList<>();
        List<Integer> targetSizes = targetGroupSizes(assignedEntries.size(), numberOfGroups);
        for (int index = 0; index < numberOfGroups; index++) {
            groups.add(new ArrayList<>());
        }

        assignedEntries.sort(Comparator
                .comparingInt((TournamentEntry entry) -> normalizedPreferenceSlots(entry).size())
                .reversed()
                .thenComparing(entry -> tournamentMapper.displayTeamName(entry)));

        for (TournamentEntry entry : assignedEntries) {
            int bestGroupIndex = 0;
            int bestScore = Integer.MIN_VALUE;
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                List<TournamentEntry> groupEntries = groups.get(groupIndex);
                if (groupEntries.size() >= targetSizes.get(groupIndex)) {
                    continue;
                }

                int score = compatibilityScore(entry, groupEntries);
                if (score > bestScore || (score == bestScore && groupEntries.size() < groups.get(bestGroupIndex).size())) {
                    bestScore = score;
                    bestGroupIndex = groupIndex;
                }
            }

            groups.get(bestGroupIndex).add(entry);
            char groupLetter = (char) ('A' + bestGroupIndex);
            entry.setGroupLabel("Grupo " + groupLetter);
        }
        return assignedEntries;
    }

    private List<TournamentEntry> copyEntries(List<TournamentEntry> entries) {
        return entries.stream()
                .map(entry -> TournamentEntry.builder()
                        .tournament(entry.getTournament())
                        .primaryPlayerProfile(entry.getPrimaryPlayerProfile())
                        .secondaryPlayerProfile(entry.getSecondaryPlayerProfile())
                        .createdBy(entry.getCreatedBy())
                        .teamName(entry.getTeamName())
                        .groupLabel(entry.getGroupLabel())
                        .entryKind(entry.getEntryKind())
                        .status(entry.getStatus())
                        .timePreferences(entry.getTimePreferences() == null ? List.of() : List.copyOf(entry.getTimePreferences()))
                        .createdAt(entry.getCreatedAt())
                        .build())
                .toList();
    }

    private List<TournamentLaunchPreviewGroupResponse> buildPreviewGroups(List<TournamentEntry> entries, String defaultGroupName) {
        Map<String, List<TournamentEntry>> entriesByGroup = new LinkedHashMap<>();
        for (TournamentEntry entry : entries) {
            String groupName = trimToNull(entry.getGroupLabel());
            entriesByGroup.computeIfAbsent(groupName == null ? defaultGroupName : groupName, ignored -> new ArrayList<>()).add(entry);
        }

        return entriesByGroup.entrySet().stream()
                .map(group -> new TournamentLaunchPreviewGroupResponse(
                        group.getKey(),
                        group.getValue().stream().map(this::toPreviewTeamResponse).toList()
                ))
                .toList();
    }

    private TournamentLaunchPreviewGroupResponse buildPreviewGroup(String name, List<TournamentEntry> entries) {
        return new TournamentLaunchPreviewGroupResponse(
                name,
                entries.stream().map(this::toPreviewTeamResponse).toList()
        );
    }

    private TournamentLaunchPreviewTeamResponse toPreviewTeamResponse(TournamentEntry entry) {
        List<String> memberNames = new ArrayList<>();
        if (entry.getPrimaryPlayerProfile() != null) {
            memberNames.add(entry.getPrimaryPlayerProfile().getFullName());
        }
        if (entry.getSecondaryPlayerProfile() != null) {
            memberNames.add(entry.getSecondaryPlayerProfile().getFullName());
        }

        return new TournamentLaunchPreviewTeamResponse(
                tournamentMapper.displayTeamName(entry),
                memberNames
        );
    }

    private TournamentLaunchPreviewMatchResponse toPreviewMatchResponse(TournamentMatch match) {
        return new TournamentLaunchPreviewMatchResponse(
                match.getPhase(),
                match.getRoundLabel(),
                tournamentMapper.displayTeamName(match.getTeamOneEntry()),
                tournamentMapper.displayTeamName(match.getTeamTwoEntry()),
                match.getScheduledAt(),
                match.getCourtName(),
                false
        );
    }

    private List<TournamentLaunchPreviewMatchResponse> buildEliminationPlayoffPreview(
            Tournament tournament,
            List<TournamentEntry> groupedEntries,
            List<TournamentMatch> stageMatches,
            int availableCourts,
            List<String> courtNames
    ) {
        Map<String, List<TournamentEntry>> entriesByGroup = new LinkedHashMap<>();
        groupedEntries.forEach(entry -> entriesByGroup.computeIfAbsent(entry.getGroupLabel(), ignored -> new ArrayList<>()).add(entry));

        List<TournamentLaunchPreviewMatchResponse> playoffs = new ArrayList<>();
        if (entriesByGroup.size() == 1) {
            List<TournamentEntry> groupEntries = entriesByGroup.values().stream().findFirst().orElse(List.of());
            if (groupEntries.size() >= 4) {
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.SEMIFINAL, "Semifinal 1", "1ro Grupo A", "4to Grupo A", 0));
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.SEMIFINAL, "Semifinal 2", "2do Grupo A", "3ro Grupo A", 1));
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.FINAL, "Final", "Ganador Semi 1", "Ganador Semi 2", 0));
            } else if (groupEntries.size() >= 2) {
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.FINAL, "Final", "1ro Grupo A", "2do Grupo A", 0));
            }
            return playoffs;
        }

        if (entriesByGroup.size() == 2) {
            boolean everyGroupHasAtLeastTwoTeams = entriesByGroup.values().stream().allMatch(groupEntries -> groupEntries.size() >= 2);
            if (everyGroupHasAtLeastTwoTeams) {
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.SEMIFINAL, "Semifinal 1", "1ro Grupo A", "2do Grupo B", 0));
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.SEMIFINAL, "Semifinal 2", "1ro Grupo B", "2do Grupo A", 1));
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.FINAL, "Final", "Ganador Semi 1", "Ganador Semi 2", 0));
            } else {
                playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                        TournamentMatchPhase.FINAL, "Final", "1ro Grupo A", "1ro Grupo B", 0));
            }
            return playoffs;
        }

        if (entriesByGroup.size() == 4) {
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.SEMIFINAL, "Semifinal 1", "Mejor 1ro", "4to mejor 1ro", 0));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.SEMIFINAL, "Semifinal 2", "2do mejor 1ro", "3er mejor 1ro", 1));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.FINAL, "Final", "Ganador Semi 1", "Ganador Semi 2", 0));
            return playoffs;
        }

        if (entriesByGroup.size() == 8) {
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.QUARTERFINAL, "Cuartos de Final 1", "Mejor 1ro", "8vo mejor 1ro", 0));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.QUARTERFINAL, "Cuartos de Final 2", "4to mejor 1ro", "5to mejor 1ro", 1));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.QUARTERFINAL, "Cuartos de Final 3", "2do mejor 1ro", "7mo mejor 1ro", 2));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.QUARTERFINAL, "Cuartos de Final 4", "3er mejor 1ro", "6to mejor 1ro", 3));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.SEMIFINAL, "Semifinal 1", "Ganador QF 1", "Ganador QF 2", 0));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.SEMIFINAL, "Semifinal 2", "Ganador QF 3", "Ganador QF 4", 1));
            playoffs.add(buildPlayoffPreviewMatch(tournament, stageMatches, playoffs, availableCourts, courtNames,
                    TournamentMatchPhase.FINAL, "Final", "Ganador Semi 1", "Ganador Semi 2", 0));
        }

        return playoffs;
    }

    private TournamentLaunchPreviewMatchResponse buildPlayoffPreviewMatch(
            Tournament tournament,
            List<TournamentMatch> stageMatches,
            List<TournamentLaunchPreviewMatchResponse> existingPlayoffs,
            int availableCourts,
            List<String> courtNames,
            TournamentMatchPhase phase,
            String roundLabel,
            String teamOneLabel,
            String teamTwoLabel,
            int slotOffset
    ) {
        Instant lastScheduledAt = stageMatches.stream()
                .map(TournamentMatch::getScheduledAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(tournament.getStartDate().atTime(18, 0).toInstant(ZoneOffset.UTC));
        Instant lastPreviewScheduledAt = existingPlayoffs.stream()
                .map(TournamentLaunchPreviewMatchResponse::scheduledAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(lastScheduledAt);

        LocalDate baseDate = lastPreviewScheduledAt.atZone(ZoneOffset.UTC).toLocalDate().plusDays(1);
        LocalTime baseTime = switch (phase) {
            case FINAL -> LocalTime.of(19, 0);
            case SEMIFINAL -> LocalTime.of(18 + (slotOffset * 2), 0);
            case QUARTERFINAL -> LocalTime.of(16 + (slotOffset * 2), 0);
            default -> LocalTime.of(18, 0);
        };
        Instant scheduledAt = baseDate.atTime(baseTime).toInstant(ZoneOffset.UTC);

        return new TournamentLaunchPreviewMatchResponse(
                phase,
                roundLabel,
                teamOneLabel,
                teamTwoLabel,
                scheduledAt,
                courtNames.get(Math.min(slotOffset % availableCourts, courtNames.size() - 1)),
                true
        );
    }

    private List<TournamentMatch> generateEliminationGroupStageMatches(
            Tournament tournament,
            List<TournamentEntry> groupedEntries,
            int availableCourts,
            List<String> courtNames
    ) {
        Map<String, List<TournamentEntry>> entriesByGroup = new LinkedHashMap<>();
        groupedEntries.forEach(entry -> entriesByGroup.computeIfAbsent(entry.getGroupLabel(), ignored -> new ArrayList<>()).add(entry));

        List<MatchSeed> seeds = new ArrayList<>();
        int roundNumber = 1;

        for (Map.Entry<String, List<TournamentEntry>> group : entriesByGroup.entrySet()) {
            List<TournamentEntry> groupEntries = group.getValue();
            for (int leftIndex = 0; leftIndex < groupEntries.size(); leftIndex++) {
                for (int rightIndex = leftIndex + 1; rightIndex < groupEntries.size(); rightIndex++) {
                    seeds.add(new MatchSeed(
                            groupEntries.get(leftIndex),
                            groupEntries.get(rightIndex),
                            TournamentMatchPhase.GROUP_STAGE,
                            roundNumber++,
                            null,
                            "Fase de Grupos - " + group.getKey()
                    ));
                }
            }
        }

        return scheduleMatchSeeds(tournament, seeds, availableCourts, courtNames);
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
        long availableSlots = availableDays * availableCourts * TimeBand.values().length;

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
        long availableSlots = availableDays * availableCourts * TimeBand.values().length;

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
        List<MatchSeed> seeds = new ArrayList<>();

        int teams = current.size();
        int matchesPerRound = teams / 2;

        for (int round = 0; round < matchesPerParticipant; round++) {
            for (int pair = 0; pair < matchesPerRound; pair++) {
                TournamentEntry teamOne = current.get(pair);
                TournamentEntry teamTwo = current.get(teams - 1 - pair);
                seeds.add(new MatchSeed(
                        teamOne,
                        teamTwo,
                        TournamentMatchPhase.AMERICANO_STAGE,
                        round + 1,
                        null,
                        "Ronda " + (round + 1)
                ));
            }
            current = rotateRoundRobin(current);
        }

        return scheduleMatchSeeds(tournament, seeds, availableCourts, courtNames);
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

    private List<Integer> targetGroupSizes(int teamCount, int numberOfGroups) {
        int baseSize = teamCount / numberOfGroups;
        int remainder = teamCount % numberOfGroups;
        List<Integer> sizes = new ArrayList<>();
        for (int index = 0; index < numberOfGroups; index++) {
            sizes.add(baseSize + (index < remainder ? 1 : 0));
        }
        return sizes;
    }

    private int compatibilityScore(TournamentEntry candidate, List<TournamentEntry> groupEntries) {
        Set<PreferenceSlot> candidatePreferences = normalizedPreferenceSlots(candidate);
        if (candidatePreferences.isEmpty() || groupEntries.isEmpty()) {
            return 0;
        }

        int score = 0;
        for (TournamentEntry existing : groupEntries) {
            Set<PreferenceSlot> existingPreferences = normalizedPreferenceSlots(existing);
            if (existingPreferences.isEmpty()) {
                continue;
            }
            score += sharedPreferenceCount(candidatePreferences, existingPreferences);
        }
        return score;
    }

    private List<TournamentMatch> scheduleMatchSeeds(
            Tournament tournament,
            List<MatchSeed> seeds,
            int availableCourts,
            List<String> courtNames
    ) {
        List<ScheduleSlot> candidateSlots = buildScheduleSlots(tournament, availableCourts, courtNames);
        Set<String> occupiedSlotKeys = new HashSet<>(occupiedSlotKeysFromOtherTournaments(tournament));
        Map<Long, Set<LocalDate>> scheduledDatesByEntryId = new HashMap<>();
        List<TournamentMatch> matches = new ArrayList<>();

        for (MatchSeed seed : seeds) {
            ScheduleSlot slot = selectBestSlot(seed, candidateSlots, occupiedSlotKeys, scheduledDatesByEntryId)
                    .orElseThrow(() -> new ConflictException("Tournament launch could not find enough compatible court/time slots"));

            occupiedSlotKeys.add(slot.key());
            scheduledDatesByEntryId.computeIfAbsent(seed.teamOne().getId(), ignored -> new HashSet<>()).add(slot.date());
            scheduledDatesByEntryId.computeIfAbsent(seed.teamTwo().getId(), ignored -> new HashSet<>()).add(slot.date());
            matches.add(buildTournamentMatch(seed, slot));
        }

        return matches;
    }

    private Optional<ScheduleSlot> selectBestSlot(
            MatchSeed seed,
            List<ScheduleSlot> candidateSlots,
            Set<String> occupiedSlotKeys,
            Map<Long, Set<LocalDate>> scheduledDatesByEntryId
    ) {
        Set<PreferenceSlot> teamOnePreferences = normalizedPreferenceSlots(seed.teamOne());
        Set<PreferenceSlot> teamTwoPreferences = normalizedPreferenceSlots(seed.teamTwo());
        boolean teamOneHasPreferences = !teamOnePreferences.isEmpty();
        boolean teamTwoHasPreferences = !teamTwoPreferences.isEmpty();

        return candidateSlots.stream()
                .filter(slot -> !occupiedSlotKeys.contains(slot.key()))
                .sorted((left, right) -> {
                    int leftScore = slotScore(left, teamOnePreferences, teamTwoPreferences, teamOneHasPreferences, teamTwoHasPreferences, seed, scheduledDatesByEntryId);
                    int rightScore = slotScore(right, teamOnePreferences, teamTwoPreferences, teamOneHasPreferences, teamTwoHasPreferences, seed, scheduledDatesByEntryId);
                    if (leftScore != rightScore) {
                        return Integer.compare(rightScore, leftScore);
                    }
                    return left.compareChronologically(right);
                })
                .findFirst();
    }

    private int slotScore(
            ScheduleSlot slot,
            Set<PreferenceSlot> teamOnePreferences,
            Set<PreferenceSlot> teamTwoPreferences,
            boolean teamOneHasPreferences,
            boolean teamTwoHasPreferences,
            MatchSeed seed,
            Map<Long, Set<LocalDate>> scheduledDatesByEntryId
    ) {
        PreferenceSlot preferenceSlot = new PreferenceSlot(slot.date(), slot.timeBand());
        boolean teamOneMatches = teamOnePreferences.contains(preferenceSlot);
        boolean teamTwoMatches = teamTwoPreferences.contains(preferenceSlot);
        boolean oneTeamAlreadyScheduledThatDay = scheduledDatesByEntryId.getOrDefault(seed.teamOne().getId(), Set.of()).contains(slot.date())
                || scheduledDatesByEntryId.getOrDefault(seed.teamTwo().getId(), Set.of()).contains(slot.date());

        int score = 0;
        if (teamOneMatches && teamTwoMatches) {
            score += 40;
        } else if (teamOneMatches || teamTwoMatches) {
            score += 20;
        } else if (!teamOneHasPreferences && !teamTwoHasPreferences) {
            score += 10;
        } else if (teamOneHasPreferences != teamTwoHasPreferences) {
            score += 5;
        }

        if (!oneTeamAlreadyScheduledThatDay) {
            score += 3;
        }

        return score;
    }

    private TournamentMatch buildTournamentMatch(MatchSeed seed, ScheduleSlot slot) {
        return TournamentMatch.builder()
                .tournament(slot.tournament())
                .teamOneEntry(seed.teamOne())
                .teamTwoEntry(seed.teamTwo())
                .phase(seed.phase())
                .status(TournamentMatchStatus.SCHEDULED)
                .roundNumber(seed.roundNumber())
                .legNumber(seed.legNumber())
                .roundLabel(seed.roundLabel())
                .scheduledAt(slot.scheduledAt())
                .courtName(slot.courtName())
                .build();
    }

    private List<ScheduleSlot> buildScheduleSlots(Tournament tournament, int availableCourts, List<String> courtNames) {
        LocalDate endDate = tournament.getEndDate() == null ? tournament.getStartDate() : tournament.getEndDate();
        List<ScheduleSlot> slots = new ArrayList<>();
        for (LocalDate date = tournament.getStartDate(); !date.isAfter(endDate); date = date.plusDays(1)) {
            for (TimeBand timeBand : TimeBand.values()) {
                for (int courtIndex = 0; courtIndex < availableCourts; courtIndex++) {
                    String courtName = courtNames.get(Math.min(courtIndex, courtNames.size() - 1));
                    Instant scheduledAt = date.atTime(timeBand.localTime()).toInstant(ZoneOffset.UTC);
                    slots.add(new ScheduleSlot(
                            tournament,
                            date,
                            timeBand,
                            courtName,
                            scheduledAt,
                            scheduleSlotKey(scheduledAt, courtName)
                    ));
                }
            }
        }
        return slots;
    }

    private Set<String> occupiedSlotKeysFromOtherTournaments(Tournament tournament) {
        if (tournament.getClub() == null || tournament.getClub().getId() == null || tournament.getId() == null) {
            return Set.of();
        }

        return tournamentMatchRepository.findAllScheduledByClubIdExcludingTournamentId(tournament.getClub().getId(), tournament.getId()).stream()
                .map(match -> scheduleSlotKey(match.getScheduledAt(), match.getCourtName()))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private String scheduleSlotKey(Instant scheduledAt, String courtName) {
        if (scheduledAt == null || courtName == null || courtName.isBlank()) {
            return null;
        }
        return scheduledAt.toString() + "|" + courtName.trim();
    }

    private Set<PreferenceSlot> normalizedPreferenceSlots(TournamentEntry entry) {
        if (entry == null || entry.getTimePreferences() == null || entry.getTimePreferences().isEmpty()) {
            return Set.of();
        }

        Set<PreferenceSlot> slots = new HashSet<>();
        for (String value : entry.getTimePreferences()) {
            parsePreferenceSlot(value).ifPresent(slots::add);
        }
        return slots;
    }

    private Optional<PreferenceSlot> parsePreferenceSlot(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return Optional.empty();
        }

        String[] parts = normalized.split("\\|");
        if (parts.length != 2) {
            return Optional.empty();
        }

        try {
            LocalDate date = LocalDate.parse(parts[0].trim());
            TimeBand timeBand = TimeBand.valueOf(parts[1].trim());
            return Optional.of(new PreferenceSlot(date, timeBand));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private int sharedPreferenceCount(Set<PreferenceSlot> left, Set<PreferenceSlot> right) {
        int count = 0;
        for (PreferenceSlot slot : left) {
            if (right.contains(slot)) {
                count++;
            }
        }
        return count;
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

    private record MatchSeed(
            TournamentEntry teamOne,
            TournamentEntry teamTwo,
            TournamentMatchPhase phase,
            int roundNumber,
            Integer legNumber,
            String roundLabel
    ) {
    }

    private record RegistrationDraft(
            PlayerProfile primaryPlayer,
            PlayerProfile secondaryPlayer,
            String teamName,
            List<String> timePreferences
    ) {
        private int memberCount() {
            return secondaryPlayer == null ? 1 : 2;
        }
    }

    private record PreferenceSlot(LocalDate date, TimeBand timeBand) {
    }

    private record ScheduleSlot(
            Tournament tournament,
            LocalDate date,
            TimeBand timeBand,
            String courtName,
            Instant scheduledAt,
            String key
    ) {
        private int compareChronologically(ScheduleSlot other) {
            int byInstant = scheduledAt.compareTo(other.scheduledAt);
            if (byInstant != 0) {
                return byInstant;
            }
            return courtName.compareTo(other.courtName);
        }
    }

    private enum TimeBand {
        MORNING(LocalTime.of(10, 0)),
        AFTERNOON(LocalTime.of(15, 0)),
        EVENING(LocalTime.of(20, 0));

        private final LocalTime localTime;

        TimeBand(LocalTime localTime) {
            this.localTime = localTime;
        }

        private LocalTime localTime() {
            return localTime;
        }
    }

    private record GeneratedPairing(
            AmericanoDynamicMatchup matchup,
            TournamentEntry teamOneEntry,
            TournamentEntry teamTwoEntry
    ) {
    }
}
