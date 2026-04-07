package com.sentimospadel.backend.club.service;

import com.sentimospadel.backend.club.dto.ClubAgendaSlotActionRequest;
import com.sentimospadel.backend.club.dto.ClubManagementActivityResponse;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaCourtResponse;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubManagementAgendaSlotResponse;
import com.sentimospadel.backend.club.dto.ClubManagementCourtResponse;
import com.sentimospadel.backend.club.dto.ClubManagementCourtsResponse;
import com.sentimospadel.backend.club.dto.ClubManagementDashboardResponse;
import com.sentimospadel.backend.club.dto.ClubManagementTopUserResponse;
import com.sentimospadel.backend.club.dto.ClubManagementUsersResponse;
import com.sentimospadel.backend.club.dto.ClubQuickActionRequest;
import com.sentimospadel.backend.club.dto.ClubQuickActionResponse;
import com.sentimospadel.backend.club.dto.CreateClubCourtRequest;
import com.sentimospadel.backend.club.dto.ReorderClubCourtsRequest;
import com.sentimospadel.backend.club.dto.UpdateClubCourtRequest;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.entity.ClubActivityLog;
import com.sentimospadel.backend.club.entity.ClubAgendaSlotOverride;
import com.sentimospadel.backend.club.entity.ClubCourt;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotActionType;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.enums.ClubQuickActionType;
import com.sentimospadel.backend.club.repository.ClubActivityLogRepository;
import com.sentimospadel.backend.club.repository.ClubAgendaSlotOverrideRepository;
import com.sentimospadel.backend.club.repository.ClubCourtRepository;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubManagementService {

    private static final ZoneId CLUB_ZONE = ZoneId.of("America/Montevideo");
    private static final List<LocalTime> DEFAULT_SLOT_TIMES = List.of(
            LocalTime.of(16, 0),
            LocalTime.of(17, 30),
            LocalTime.of(19, 0),
            LocalTime.of(20, 30),
            LocalTime.of(22, 0)
    );
    private static final BigDecimal SLOT_DURATION_HOURS = BigDecimal.valueOf(1.5);

    private final UserRepository userRepository;
    private final ClubCourtRepository clubCourtRepository;
    private final ClubAgendaSlotOverrideRepository clubAgendaSlotOverrideRepository;
    private final ClubActivityLogRepository clubActivityLogRepository;
    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    @Transactional(readOnly = true)
    public ClubManagementDashboardResponse getDashboard(String authenticatedEmail) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        List<ClubCourt> courts = getManagedCourts(managedClub.getId());
        List<ClubCourt> activeCourts = filterActiveCourts(courts);
        LocalDate today = LocalDate.now(CLUB_ZONE);
        ClubManagementAgendaResponse todayAgenda = buildAgenda(managedClub, activeCourts, today);
        int activeCourtsCount = activeCourts.size();

        BigDecimal todayRevenue = activeCourts.stream()
                .flatMap(court -> todayAgenda.courts().stream()
                        .filter(courtResponse -> courtResponse.id().equals(court.getId()))
                        .flatMap(courtResponse -> courtResponse.slots().stream()
                                .filter(slot -> slot.status() == ClubAgendaSlotStatus.RESERVED)
                                .map(slot -> court.getHourlyRateUyu().multiply(SLOT_DURATION_HOURS))))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int todayReservationsCount = (int) todayAgenda.courts().stream()
                .flatMap(court -> court.slots().stream())
                .filter(slot -> slot.status() == ClubAgendaSlotStatus.RESERVED)
                .count();

        List<ClubManagementActivityResponse> recentActivities = clubActivityLogRepository
                .findTop8ByClubIdOrderByOccurredAtDesc(managedClub.getId())
                .stream()
                .map(activity -> new ClubManagementActivityResponse(
                        activity.getId(),
                        activity.getTitle(),
                        activity.getDescription(),
                        activity.getOccurredAt()
                ))
                .toList();

        return new ClubManagementDashboardResponse(
                managedClub.getId(),
                managedClub.getName(),
                activeCourtsCount,
                courts.size(),
                todayRevenue,
                todayReservationsCount,
                recentActivities
        );
    }

    @Transactional(readOnly = true)
    public ClubManagementUsersResponse getUsersOverview(String authenticatedEmail) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        List<MatchParticipant> clubParticipants = matchParticipantRepository
                .findAllByMatchClubIdInOrderByMatchScheduledAtDesc(Set.of(managedClub.getId()));

        YearMonth currentMonth = YearMonth.now(CLUB_ZONE);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate activeThreshold = LocalDate.now(CLUB_ZONE).minusDays(60);
        LocalDate annualThreshold = currentMonth.atDay(1).minusMonths(11);

        Map<Long, UserActivityAccumulator> activityByPlayerProfileId = new LinkedHashMap<>();

        for (MatchParticipant participant : clubParticipants) {
            Match match = participant.getMatch();
            if (match == null || match.getStatus() == MatchStatus.CANCELLED) {
                continue;
            }

            LocalDate localDate = toLocalDate(match.getScheduledAt());
            UserActivityAccumulator accumulator = activityByPlayerProfileId.computeIfAbsent(
                    participant.getPlayerProfile().getId(),
                    ignored -> new UserActivityAccumulator(
                            participant.getPlayerProfile().getId(),
                            participant.getPlayerProfile().getFullName(),
                            participant.getPlayerProfile().getPhotoUrl()
                    )
            );

            accumulator.registerParticipation(localDate, currentMonth, previousMonth, annualThreshold);
        }

        int activeUsersCount = (int) activityByPlayerProfileId.values().stream()
                .filter(accumulator -> accumulator.lastPlayedAt != null && !accumulator.lastPlayedAt.isBefore(activeThreshold))
                .count();

        int newUsersThisMonthCount = (int) activityByPlayerProfileId.values().stream()
                .filter(accumulator -> accumulator.firstPlayedAt != null && YearMonth.from(accumulator.firstPlayedAt).equals(currentMonth))
                .count();

        int inactiveUsersCount = (int) activityByPlayerProfileId.values().stream()
                .filter(accumulator -> accumulator.lastPlayedAt != null && accumulator.lastPlayedAt.isBefore(activeThreshold))
                .count();

        BigDecimal currentMonthRevenue = estimateMonthlyRevenue(managedClub.getId(), getManagedCourts(managedClub.getId()), currentMonth);
        int currentMonthPlayers = (int) activityByPlayerProfileId.values().stream()
                .filter(accumulator -> accumulator.matchesThisMonth > 0)
                .count();

        List<ClubManagementTopUserResponse> topUsers = activityByPlayerProfileId.values().stream()
                .filter(accumulator -> accumulator.matchesThisMonth > 0)
                .sorted(Comparator
                        .comparingInt(UserActivityAccumulator::matchesThisMonth).reversed()
                        .thenComparing(UserActivityAccumulator::fullName))
                .limit(10)
                .map(new RankedUserMapper())
                .toList();

        return new ClubManagementUsersResponse(
                managedClub.getId(),
                managedClub.getName(),
                activeUsersCount,
                newUsersThisMonthCount,
                inactiveUsersCount,
                divideOrZero(currentMonthRevenue, currentMonthPlayers, 2),
                averageMatches(activityByPlayerProfileId.values(), UserActivityAccumulator::matchesThisMonth),
                averageMatches(activityByPlayerProfileId.values(), UserActivityAccumulator::matchesPreviousMonth),
                averageMatches(activityByPlayerProfileId.values(), UserActivityAccumulator::matchesYear),
                topUsers
        );
    }

    @Transactional(readOnly = true)
    public ClubManagementAgendaResponse getAgenda(String authenticatedEmail, LocalDate date) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        return buildAgenda(managedClub, getManagedActiveCourts(managedClub.getId()), date);
    }

    @Transactional(readOnly = true)
    public ClubManagementCourtsResponse getCourts(String authenticatedEmail) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        return buildCourtsResponse(managedClub, getManagedCourts(managedClub.getId()));
    }

    @Transactional
    public ClubManagementCourtsResponse createCourt(String authenticatedEmail, CreateClubCourtRequest request) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        List<ClubCourt> courts = getManagedCourts(managedClub.getId());
        String requestedName = requireCourtName(request.name());

        ensureCourtNameAvailable(courts, requestedName, null);

        ClubCourt court = ClubCourt.builder()
                .club(managedClub)
                .name(requestedName)
                .displayOrder(courts.stream()
                        .map(ClubCourt::getDisplayOrder)
                        .max(Integer::compareTo)
                        .orElse(0) + 1)
                .hourlyRateUyu(scaleCurrency(request.hourlyRateUyu()))
                .active(true)
                .build();

        clubCourtRepository.save(court);
        registerActivity(managedClub, "Cancha creada", court.getName() + " - $" + formatCurrency(court.getHourlyRateUyu()));
        return buildCourtsResponse(managedClub, getManagedCourts(managedClub.getId()));
    }

    @Transactional
    public ClubManagementCourtsResponse updateCourt(String authenticatedEmail, Long courtId, UpdateClubCourtRequest request) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        List<ClubCourt> courts = getManagedCourts(managedClub.getId());
        ClubCourt court = courts.stream()
                .filter(item -> item.getId().equals(courtId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Court with id " + courtId + " was not found"));

        String requestedName = requireCourtName(request.name());
        BigDecimal requestedRate = scaleCurrency(request.hourlyRateUyu());
        boolean requestedActive = request.active();
        boolean nameChanged = !court.getName().equals(requestedName);
        boolean rateChanged = court.getHourlyRateUyu().compareTo(requestedRate) != 0;
        boolean activeChanged = court.isActive() != requestedActive;

        if (nameChanged) {
            ensureCourtNameAvailable(courts, requestedName, court.getId());

            if (hasFutureRealMatches(managedClub.getId(), courts, court)
                    && !simplifyCourtName(court.getName()).equals(simplifyCourtName(requestedName))) {
                throw new ConflictException("This court has future real matches and cannot be renamed until they are rescheduled");
            }
        }

        if (court.isActive() && !requestedActive && hasFutureScheduledOccupancy(managedClub.getId(), courts, court)) {
            throw new ConflictException("This court has future reservations or matches and cannot be deactivated");
        }

        court.setName(requestedName);
        court.setHourlyRateUyu(requestedRate);
        court.setActive(requestedActive);
        clubCourtRepository.save(court);

        if (activeChanged) {
            registerActivity(
                    managedClub,
                    requestedActive ? "Cancha reactivada" : "Cancha desactivada",
                    court.getName() + " - $" + formatCurrency(court.getHourlyRateUyu())
            );
        } else if (nameChanged || rateChanged) {
            registerActivity(
                    managedClub,
                    "Cancha actualizada",
                    court.getName() + " - $" + formatCurrency(court.getHourlyRateUyu())
            );
        }

        return buildCourtsResponse(managedClub, getManagedCourts(managedClub.getId()));
    }

    @Transactional
    public ClubManagementCourtsResponse reorderCourts(String authenticatedEmail, ReorderClubCourtsRequest request) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        List<ClubCourt> courts = getManagedCourts(managedClub.getId());

        if (request.orderedCourtIds().size() != courts.size()
                || new HashSet<>(request.orderedCourtIds()).size() != courts.size()
                || !new HashSet<>(request.orderedCourtIds()).equals(courts.stream().map(ClubCourt::getId).collect(java.util.stream.Collectors.toSet()))) {
            throw new ConflictException("Court reorder payload must include every managed court exactly once");
        }

        Map<Long, ClubCourt> courtById = courts.stream()
                .collect(LinkedHashMap::new, (map, court) -> map.put(court.getId(), court), LinkedHashMap::putAll);

        for (int index = 0; index < request.orderedCourtIds().size(); index++) {
            courtById.get(request.orderedCourtIds().get(index)).setDisplayOrder(index + 1);
        }

        clubCourtRepository.saveAll(courtById.values());
        registerActivity(managedClub, "Orden de canchas actualizado", "Se actualizo el orden visible del club");
        return buildCourtsResponse(managedClub, getManagedCourts(managedClub.getId()));
    }

    @Transactional
    public ClubManagementAgendaResponse applyAgendaSlotAction(String authenticatedEmail, ClubAgendaSlotActionRequest request) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        List<ClubCourt> courts = getManagedActiveCourts(managedClub.getId());
        ClubCourt court = courts.stream()
                .filter(item -> item.getId().equals(request.courtId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Court with id " + request.courtId() + " was not found"));

        if (!DEFAULT_SLOT_TIMES.contains(request.time())) {
            throw new ConflictException("Only the configured club agenda slots can be managed from this view");
        }

        ClubAgendaSlotOverride existingOverride = clubAgendaSlotOverrideRepository
                .findByClubIdAndCourtIdAndSlotDateAndStartTime(managedClub.getId(), court.getId(), request.date(), request.time())
                .orElse(null);

        boolean occupiedByRealMatch = isOccupiedByRealMatch(managedClub.getId(), courts, request.date(), request.time(), court.getId());

        if (request.action() == ClubAgendaSlotActionType.FREE) {
            if (existingOverride == null) {
                if (occupiedByRealMatch) {
                    throw new ConflictException("Social match reservations cannot be released from the club agenda");
                }

                throw new ConflictException("This slot is already free");
            }

            clubAgendaSlotOverrideRepository.delete(existingOverride);
            registerActivity(managedClub, "Cancha liberada", court.getName() + " • " + request.time());
            return buildAgenda(managedClub, courts, request.date());
        }

        if (occupiedByRealMatch) {
            throw new ConflictException("This slot is already reserved by a real club match");
        }

        ClubAgendaSlotStatus nextStatus = request.action() == ClubAgendaSlotActionType.BLOCK
                ? ClubAgendaSlotStatus.BLOCKED
                : ClubAgendaSlotStatus.RESERVED;

        ClubAgendaSlotOverride override = existingOverride != null ? existingOverride : ClubAgendaSlotOverride.builder()
                .club(managedClub)
                .court(court)
                .slotDate(request.date())
                .startTime(request.time())
                .build();

        override.setStatus(nextStatus);
        override.setReservedByName(nextStatus == ClubAgendaSlotStatus.RESERVED
                ? Optional.ofNullable(trimToNull(request.reservedByName())).orElse("Reserva Manual")
                : null);

        clubAgendaSlotOverrideRepository.save(override);

        if (nextStatus == ClubAgendaSlotStatus.RESERVED) {
            registerActivity(managedClub, "Reserva manual", court.getName() + " • " + request.time() + " • " + override.getReservedByName());
        } else {
            registerActivity(managedClub, "Cancha bloqueada", court.getName() + " • " + request.time());
        }

        return buildAgenda(managedClub, courts, request.date());
    }

    @Transactional
    public ClubQuickActionResponse executeQuickAction(String authenticatedEmail, ClubQuickActionRequest request) {
        Club managedClub = resolveManagedClub(authenticatedEmail);
        String message = switch (request.type()) {
            case NOTIFY_USERS -> "Registro operativo guardado: aviso a usuarios";
            case ACTIVATE_RESERVATION_PROMO -> "Registro operativo guardado: promocion de reservas";
            case ACTIVATE_LAST_MINUTE_DISCOUNT -> "Registro operativo guardado: descuento de ultimo minuto";
        };

        registerActivity(managedClub, quickActionTitle(request.type()), message);
        return new ClubQuickActionResponse(message);
    }

    private ClubManagementAgendaResponse buildAgenda(Club managedClub, List<ClubCourt> courts, LocalDate date) {
        List<ClubAgendaSlotOverride> overrides = clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDate(managedClub.getId(), date);
        Instant rangeStart = date.atStartOfDay(CLUB_ZONE).toInstant();
        Instant rangeEnd = date.plusDays(1).atStartOfDay(CLUB_ZONE).toInstant();
        List<Match> clubMatches = matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                managedClub.getId(),
                rangeStart,
                rangeEnd
        );
        List<MatchParticipant> participants = loadParticipants(clubMatches);

        Map<SlotKey, SlotOccupancy> occupancyBySlot = buildOccupancyMap(courts, clubMatches, participants, overrides);
        List<ClubManagementAgendaCourtResponse> courtResponses = courts.stream()
                .map(court -> new ClubManagementAgendaCourtResponse(
                        court.getId(),
                        court.getName(),
                        DEFAULT_SLOT_TIMES.stream()
                                .map(slotTime -> toSlotResponse(court, date, slotTime, occupancyBySlot.get(new SlotKey(court.getId(), date, slotTime))))
                                .toList()
                ))
                .toList();

        return new ClubManagementAgendaResponse(managedClub.getId(), managedClub.getName(), date, courtResponses);
    }

    private Map<SlotKey, SlotOccupancy> buildOccupancyMap(
            List<ClubCourt> courts,
            List<Match> clubMatches,
            List<MatchParticipant> participants,
            List<ClubAgendaSlotOverride> overrides
    ) {
        Map<Long, List<MatchParticipant>> participantsByMatchId = participants.stream()
                .collect(LinkedHashMap::new, (map, participant) -> map
                        .computeIfAbsent(participant.getMatch().getId(), ignored -> new ArrayList<>())
                        .add(participant), LinkedHashMap::putAll);

        Map<SlotKey, SlotOccupancy> occupancyBySlot = new HashMap<>();

        for (Match match : clubMatches) {
            if (match.getStatus() == MatchStatus.CANCELLED) {
                continue;
            }

            Optional<ClubCourt> maybeCourt = resolveCourtForMatch(match, courts);
            if (maybeCourt.isEmpty()) {
                continue;
            }

            LocalDate localDate = toLocalDate(match.getScheduledAt());
            LocalTime localTime = toLocalTime(match.getScheduledAt());
            if (!DEFAULT_SLOT_TIMES.contains(localTime)) {
                continue;
            }

            List<MatchParticipant> matchParticipants = participantsByMatchId.getOrDefault(match.getId(), List.of());
            String reservedByName = matchParticipants.isEmpty()
                    ? match.getCreatedBy().getFullName()
                    : matchParticipants.get(0).getPlayerProfile().getFullName();

            occupancyBySlot.put(
                    new SlotKey(maybeCourt.get().getId(), localDate, localTime),
                    new SlotOccupancy(ClubAgendaSlotStatus.RESERVED, reservedByName, true)
            );
        }

        for (ClubAgendaSlotOverride override : overrides) {
            occupancyBySlot.put(
                    new SlotKey(override.getCourt().getId(), override.getSlotDate(), override.getStartTime()),
                    new SlotOccupancy(
                            override.getStatus(),
                            override.getReservedByName(),
                            false
                    )
            );
        }

        return occupancyBySlot;
    }

    private ClubManagementAgendaSlotResponse toSlotResponse(
            ClubCourt court,
            LocalDate date,
            LocalTime slotTime,
            SlotOccupancy occupancy
    ) {
        if (occupancy == null) {
            return new ClubManagementAgendaSlotResponse(
                    buildSlotId(court.getId(), date, slotTime),
                    slotTime.toString(),
                    ClubAgendaSlotStatus.AVAILABLE,
                    null
            );
        }

        return new ClubManagementAgendaSlotResponse(
                buildSlotId(court.getId(), date, slotTime),
                slotTime.toString(),
                occupancy.status(),
                occupancy.reservedByName()
        );
    }

    private boolean isOccupiedByRealMatch(Long clubId, List<ClubCourt> courts, LocalDate date, LocalTime time, Long courtId) {
        Instant rangeStart = date.atStartOfDay(CLUB_ZONE).toInstant();
        Instant rangeEnd = date.plusDays(1).atStartOfDay(CLUB_ZONE).toInstant();
        return matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(clubId, rangeStart, rangeEnd).stream()
                .filter(match -> match.getStatus() != MatchStatus.CANCELLED)
                .map(match -> resolveCourtForMatch(match, courts)
                        .filter(court -> court.getId().equals(courtId))
                        .map(court -> toLocalTime(match.getScheduledAt()).equals(time))
                        .orElse(false))
                .anyMatch(Boolean::booleanValue);
    }

    private BigDecimal estimateMonthlyRevenue(Long clubId, List<ClubCourt> courts, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        Instant rangeStart = from.atStartOfDay(CLUB_ZONE).toInstant();
        Instant rangeEnd = to.plusDays(1).atStartOfDay(CLUB_ZONE).toInstant();
        List<Match> matches = matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(clubId, rangeStart, rangeEnd);
        List<MatchParticipant> participants = loadParticipants(matches);
        List<ClubAgendaSlotOverride> overrides = clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDateBetween(clubId, from, to);
        Map<SlotKey, SlotOccupancy> occupancyBySlot = buildOccupancyMap(courts, matches, participants, overrides);
        Map<Long, ClubCourt> courtById = courts.stream().collect(LinkedHashMap::new, (map, court) -> map.put(court.getId(), court), LinkedHashMap::putAll);

        return occupancyBySlot.entrySet().stream()
                .filter(entry -> entry.getValue().status() == ClubAgendaSlotStatus.RESERVED)
                .map(entry -> courtById.get(entry.getKey().courtId()).getHourlyRateUyu().multiply(SLOT_DURATION_HOURS))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Club resolveManagedClub(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));

        if (user.getRole() != UserRole.ADMIN || user.getManagedClub() == null) {
            throw new AccessDeniedException("Only club administrators with a managed club can access this resource");
        }

        return user.getManagedClub();
    }

    private List<ClubCourt> getManagedCourts(Long clubId) {
        return clubCourtRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId);
    }

    private List<ClubCourt> getManagedActiveCourts(Long clubId) {
        return clubCourtRepository.findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(clubId);
    }

    private List<ClubCourt> filterActiveCourts(List<ClubCourt> courts) {
        return courts.stream().filter(ClubCourt::isActive).toList();
    }

    private ClubManagementCourtsResponse buildCourtsResponse(Club managedClub, List<ClubCourt> courts) {
        return new ClubManagementCourtsResponse(
                managedClub.getId(),
                managedClub.getName(),
                (int) courts.stream().filter(ClubCourt::isActive).count(),
                courts.size(),
                courts.stream()
                        .map(court -> new ClubManagementCourtResponse(
                                court.getId(),
                                court.getName(),
                                court.getDisplayOrder(),
                                court.getHourlyRateUyu(),
                                court.isActive()
                        ))
                        .toList()
        );
    }

    private List<MatchParticipant> loadParticipants(List<Match> matches) {
        if (matches.isEmpty()) {
            return List.of();
        }

        return matchParticipantRepository.findAllByMatchIdInOrderByJoinedAtAsc(
                matches.stream().map(Match::getId).toList()
        );
    }

    private Optional<ClubCourt> resolveCourtForMatch(Match match, Collection<ClubCourt> courts) {
        String location = normalize(match.getLocationText());
        if (location == null) {
            return Optional.empty();
        }

        return courts.stream()
                .filter(court -> buildCourtAliases(court).stream().anyMatch(location::contains))
                .findFirst();
    }

    private List<String> buildCourtAliases(ClubCourt court) {
        String normalizedName = normalize(court.getName());
        String simplified = simplifyCourtName(normalizedName);
        return normalizedName.equals(simplified)
                ? List.of(normalizedName)
                : List.of(normalizedName, simplified);
    }

    private boolean hasFutureScheduledOccupancy(Long clubId, List<ClubCourt> courts, ClubCourt court) {
        return hasFutureSlotOverrides(clubId, court) || hasFutureRealMatches(clubId, courts, court);
    }

    private boolean hasFutureSlotOverrides(Long clubId, ClubCourt court) {
        LocalDate today = LocalDate.now(CLUB_ZONE);
        LocalTime now = LocalTime.now(CLUB_ZONE).withSecond(0).withNano(0);
        return clubAgendaSlotOverrideRepository.findAllByClubIdAndCourtIdAndSlotDateGreaterThanEqual(clubId, court.getId(), today).stream()
                .anyMatch(override -> override.getSlotDate().isAfter(today)
                        || !override.getStartTime().isBefore(now));
    }

    private boolean hasFutureRealMatches(Long clubId, Collection<ClubCourt> courts, ClubCourt targetCourt) {
        return matchRepository.findAllByClubIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(clubId, Instant.now()).stream()
                .filter(match -> match.getStatus() != MatchStatus.CANCELLED)
                .map(match -> resolveCourtForMatch(match, courts)
                        .filter(court -> court.getId().equals(targetCourt.getId()))
                        .isPresent())
                .anyMatch(Boolean::booleanValue);
    }

    private void ensureCourtNameAvailable(List<ClubCourt> courts, String requestedName, Long ignoredCourtId) {
        String normalizedRequestedName = normalize(requestedName);
        boolean exists = courts.stream()
                .filter(court -> ignoredCourtId == null || !court.getId().equals(ignoredCourtId))
                .map(ClubCourt::getName)
                .map(this::normalize)
                .anyMatch(normalizedRequestedName::equals);

        if (exists) {
            throw new ConflictException("Court names must be unique within the club");
        }
    }

    private void registerActivity(Club managedClub, String title, String description) {
        clubActivityLogRepository.save(ClubActivityLog.builder()
                .club(managedClub)
                .title(title)
                .description(description)
                .occurredAt(Instant.now())
                .build());
    }

    private String quickActionTitle(ClubQuickActionType type) {
        return switch (type) {
            case NOTIFY_USERS -> "Aviso registrado";
            case ACTIVATE_RESERVATION_PROMO -> "Promocion registrada";
            case ACTIVATE_LAST_MINUTE_DISCOUNT -> "Descuento registrado";
        };
    }

    private LocalDate toLocalDate(Instant instant) {
        return ZonedDateTime.ofInstant(instant, CLUB_ZONE).toLocalDate();
    }

    private LocalTime toLocalTime(Instant instant) {
        return ZonedDateTime.ofInstant(instant, CLUB_ZONE).toLocalTime().withSecond(0).withNano(0);
    }

    private String buildSlotId(Long courtId, LocalDate date, LocalTime time) {
        return courtId + "|" + date + "|" + time;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String simplifyCourtName(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }

        return normalizedValue.replaceAll("\\s*\\([^)]*\\)", "").trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireCourtName(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ConflictException("Court name must not be blank");
        }

        return trimmed;
    }

    private BigDecimal scaleCurrency(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatCurrency(BigDecimal value) {
        return scaleCurrency(value).toPlainString();
    }

    private BigDecimal divideOrZero(BigDecimal dividend, int divisor, int scale) {
        if (divisor <= 0) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }

        return dividend.divide(BigDecimal.valueOf(divisor), scale, RoundingMode.HALF_UP);
    }

    private BigDecimal averageMatches(
            Collection<UserActivityAccumulator> values,
            java.util.function.ToIntFunction<UserActivityAccumulator> extractor
    ) {
        int participantCount = (int) values.stream().filter(value -> extractor.applyAsInt(value) > 0).count();
        if (participantCount == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        int totalMatches = values.stream().mapToInt(extractor).sum();
        return BigDecimal.valueOf(totalMatches)
                .divide(BigDecimal.valueOf(participantCount), 1, RoundingMode.HALF_UP);
    }

    private record SlotKey(Long courtId, LocalDate date, LocalTime time) {
    }

    private record SlotOccupancy(
            ClubAgendaSlotStatus status,
            String reservedByName,
            boolean derivedFromRealMatch
    ) {
    }

    private static final class UserActivityAccumulator {
        private final Long playerProfileId;
        private final String fullName;
        private final String photoUrl;
        private LocalDate firstPlayedAt;
        private LocalDate lastPlayedAt;
        private int matchesThisMonth;
        private int matchesPreviousMonth;
        private int matchesYear;

        private UserActivityAccumulator(Long playerProfileId, String fullName, String photoUrl) {
            this.playerProfileId = playerProfileId;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
        }

        private void registerParticipation(LocalDate localDate, YearMonth currentMonth, YearMonth previousMonth, LocalDate annualThreshold) {
            if (firstPlayedAt == null || localDate.isBefore(firstPlayedAt)) {
                firstPlayedAt = localDate;
            }

            if (lastPlayedAt == null || localDate.isAfter(lastPlayedAt)) {
                lastPlayedAt = localDate;
            }

            YearMonth participationMonth = YearMonth.from(localDate);
            if (participationMonth.equals(currentMonth)) {
                matchesThisMonth++;
            }

            if (participationMonth.equals(previousMonth)) {
                matchesPreviousMonth++;
            }

            if (!localDate.isBefore(annualThreshold)) {
                matchesYear++;
            }
        }

        private int matchesThisMonth() {
            return matchesThisMonth;
        }

        private int matchesPreviousMonth() {
            return matchesPreviousMonth;
        }

        private int matchesYear() {
            return matchesYear;
        }

        private String fullName() {
            return fullName;
        }
    }

    private static final class RankedUserMapper implements java.util.function.Function<UserActivityAccumulator, ClubManagementTopUserResponse> {

        private int position = 0;

        @Override
        public ClubManagementTopUserResponse apply(UserActivityAccumulator accumulator) {
            position++;
            return new ClubManagementTopUserResponse(
                    position,
                    accumulator.playerProfileId,
                    accumulator.fullName,
                    accumulator.photoUrl,
                    accumulator.matchesThisMonth
            );
        }
    }
}
