package com.sentimospadel.backend.club.service;

import com.sentimospadel.backend.club.dto.ClubBookingAgendaResponse;
import com.sentimospadel.backend.club.dto.ClubBookingCourtResponse;
import com.sentimospadel.backend.club.dto.ClubBookingSlotResponse;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.club.entity.ClubAgendaSlotOverride;
import com.sentimospadel.backend.club.entity.ClubCourt;
import com.sentimospadel.backend.club.enums.ClubAgendaSlotStatus;
import com.sentimospadel.backend.club.repository.ClubAgendaSlotOverrideRepository;
import com.sentimospadel.backend.club.repository.ClubCourtRepository;
import com.sentimospadel.backend.club.repository.ClubRepository;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.shared.exception.ConflictException;
import com.sentimospadel.backend.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubBookingService {

    private static final ZoneId CLUB_ZONE = ZoneId.of("America/Montevideo");
    private static final List<LocalTime> DEFAULT_SLOT_TIMES = List.of(
            LocalTime.of(16, 0),
            LocalTime.of(17, 30),
            LocalTime.of(19, 0),
            LocalTime.of(20, 30),
            LocalTime.of(22, 0)
    );

    private final ClubRepository clubRepository;
    private final ClubCourtRepository clubCourtRepository;
    private final ClubAgendaSlotOverrideRepository clubAgendaSlotOverrideRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public ClubBookingAgendaResponse getBookingAgenda(Long clubId, LocalDate date) {
        Club club = resolveClub(clubId);
        List<ClubCourt> activeCourts = getActiveCourts(club.getId());
        Map<SlotKey, ClubAgendaSlotStatus> slotStatuses = buildSlotStatuses(club.getId(), activeCourts, date);

        return new ClubBookingAgendaResponse(
                club.getId(),
                club.getName(),
                date,
                activeCourts.stream()
                        .map(court -> new ClubBookingCourtResponse(
                                court.getId(),
                                court.getName(),
                                court.getHourlyRateUyu(),
                                DEFAULT_SLOT_TIMES.stream()
                                        .map(slotTime -> new ClubBookingSlotResponse(
                                                slotTime.toString(),
                                                slotStatuses.getOrDefault(
                                                        new SlotKey(court.getId(), date, slotTime),
                                                        ClubAgendaSlotStatus.AVAILABLE
                                                )
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public Club resolveClubBooking(Long clubId, Instant scheduledAt, String locationText) {
        if (clubId == null) {
            return null;
        }

        Club club = resolveClub(clubId);
        List<ClubCourt> activeCourts = getActiveCourts(clubId);
        if (activeCourts.isEmpty()) {
            throw new ConflictException("This club does not have bookable courts configured");
        }

        String normalizedLocation = normalize(locationText);
        if (normalizedLocation == null) {
            throw new ConflictException("A club booking must include a valid court name");
        }

        ClubCourt court = activeCourts.stream()
                .filter(candidate -> buildCourtAliases(candidate).stream().anyMatch(normalizedLocation::contains))
                .findFirst()
                .orElseThrow(() -> new ConflictException("This club booking must target one of the active configured courts"));

        ZonedDateTime localDateTime = ZonedDateTime.ofInstant(scheduledAt, CLUB_ZONE);
        LocalDate slotDate = localDateTime.toLocalDate();
        LocalTime slotTime = localDateTime.toLocalTime().withSecond(0).withNano(0);

        if (!DEFAULT_SLOT_TIMES.contains(slotTime)) {
            throw new ConflictException("This club slot is not bookable from the public booking flow");
        }

        ClubAgendaSlotStatus slotStatus = buildSlotStatuses(clubId, activeCourts, slotDate)
                .getOrDefault(new SlotKey(court.getId(), slotDate, slotTime), ClubAgendaSlotStatus.AVAILABLE);

        if (slotStatus == ClubAgendaSlotStatus.BLOCKED) {
            throw new ConflictException("This club slot is blocked");
        }

        if (slotStatus == ClubAgendaSlotStatus.RESERVED) {
            throw new ConflictException("This club slot is no longer available");
        }

        return club;
    }

    private Map<SlotKey, ClubAgendaSlotStatus> buildSlotStatuses(Long clubId, List<ClubCourt> activeCourts, LocalDate date) {
        Map<SlotKey, ClubAgendaSlotStatus> statuses = new HashMap<>();
        Instant rangeStart = date.atStartOfDay(CLUB_ZONE).toInstant();
        Instant rangeEnd = date.plusDays(1).atStartOfDay(CLUB_ZONE).toInstant();

        List<Match> clubMatches = matchRepository.findAllByClubIdAndScheduledAtBetweenOrderByScheduledAtAsc(clubId, rangeStart, rangeEnd);
        for (Match match : clubMatches) {
            if (match.getStatus() == MatchStatus.CANCELLED) {
                continue;
            }

            resolveCourtForMatch(match, activeCourts).ifPresent(court -> {
                LocalTime slotTime = toLocalTime(match.getScheduledAt());
                if (DEFAULT_SLOT_TIMES.contains(slotTime)) {
                    statuses.put(new SlotKey(court.getId(), date, slotTime), ClubAgendaSlotStatus.RESERVED);
                }
            });
        }

        List<ClubAgendaSlotOverride> overrides = clubAgendaSlotOverrideRepository.findAllByClubIdAndSlotDate(clubId, date);
        for (ClubAgendaSlotOverride override : overrides) {
            statuses.put(
                    new SlotKey(override.getCourt().getId(), override.getSlotDate(), override.getStartTime()),
                    override.getStatus()
            );
        }

        return statuses;
    }

    private Optional<ClubCourt> resolveCourtForMatch(Match match, List<ClubCourt> activeCourts) {
        String normalizedLocation = normalize(match.getLocationText());
        if (normalizedLocation == null) {
            return Optional.empty();
        }

        return activeCourts.stream()
                .filter(court -> buildCourtAliases(court).stream().anyMatch(normalizedLocation::contains))
                .findFirst();
    }

    private List<String> buildCourtAliases(ClubCourt court) {
        String normalizedName = normalize(court.getName());
        String simplifiedName = simplifyCourtName(court.getName());

        if (normalizedName == null) {
            return List.of();
        }

        return normalizedName.equals(simplifiedName)
                ? List.of(normalizedName)
                : List.of(normalizedName, simplifiedName);
    }

    private List<ClubCourt> getActiveCourts(Long clubId) {
        return clubCourtRepository.findAllByClubIdAndActiveTrueOrderByDisplayOrderAsc(clubId);
    }

    private Club resolveClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club with id " + clubId + " was not found"));
    }

    private LocalTime toLocalTime(Instant instant) {
        return ZonedDateTime.ofInstant(instant, CLUB_ZONE).toLocalTime().withSecond(0).withNano(0);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String simplifyCourtName(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }

        return normalizedValue.replaceAll("\\s*\\([^)]*\\)", "").trim();
    }

    private record SlotKey(Long courtId, LocalDate date, LocalTime time) {
    }
}
