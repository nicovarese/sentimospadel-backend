package com.sentimospadel.backend.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.dto.PlayerClubRankingSummaryResponse;
import com.sentimospadel.backend.player.dto.PlayerPartnerInsightResponse;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.rating.entity.PlayerRatingHistory;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerInsightServiceTest {

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private PlayerRatingHistoryRepository playerRatingHistoryRepository;

    private PlayerInsightService playerInsightService;

    @BeforeEach
    void setUp() {
        playerInsightService = new PlayerInsightService(
                playerProfileResolverService,
                matchParticipantRepository,
                matchResultRepository,
                playerRatingHistoryRepository
        );
    }

    @Test
    void topPartnersAggregatesConfirmedWinsAndRatingGain() {
        PlayerProfile felipe = player(3L, "Felipe Rodriguez", "felipe@example.com", "5.09");
        PlayerProfile martin = player(4L, "Martin Gomez", "martin@example.com", "4.81");
        PlayerProfile ana = player(5L, "Ana Pereira", "ana@example.com", "2.73");
        PlayerProfile rival = player(6L, "Juan Perez", "juan@example.com", "4.00");

        Match match = match(10L, MatchStatus.COMPLETED, Instant.parse("2026-03-10T20:00:00Z"), null);
        MatchParticipant felipeParticipation = participant(match, felipe, MatchParticipantTeam.TEAM_ONE);

        when(playerProfileResolverService.getOrCreateByUserEmail("felipe@example.com")).thenReturn(felipe);
        when(matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(3L))
                .thenReturn(List.of(felipeParticipation));
        when(matchParticipantRepository.findAllByMatchIdInOrderByJoinedAtAsc(List.of(10L)))
                .thenReturn(List.of(
                        felipeParticipation,
                        participant(match, martin, MatchParticipantTeam.TEAM_ONE),
                        participant(match, ana, MatchParticipantTeam.TEAM_TWO),
                        participant(match, rival, MatchParticipantTeam.TEAM_TWO)
                ));
        when(matchResultRepository.findAllByMatchIdIn(List.of(10L))).thenReturn(List.of(
                MatchResult.builder()
                        .match(match)
                        .submittedBy(felipe)
                        .status(MatchResultStatus.CONFIRMED)
                        .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                        .teamOneScore(2)
                        .teamTwoScore(0)
                        .submittedAt(Instant.parse("2026-03-10T22:00:00Z"))
                        .build()
        ));
        when(playerRatingHistoryRepository.findAllByPlayerProfileIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(
                PlayerRatingHistory.builder()
                        .playerProfile(felipe)
                        .match(match)
                        .oldRating(new BigDecimal("5.00"))
                        .delta(new BigDecimal("0.09"))
                        .newRating(new BigDecimal("5.09"))
                        .createdAt(Instant.parse("2026-03-10T22:00:00Z"))
                        .build()
        ));

        List<PlayerPartnerInsightResponse> response = playerInsightService.getMyTopPartners("felipe@example.com");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().fullName()).isEqualTo("Martin Gomez");
        assertThat(response.getFirst().matchesWonTogether()).isEqualTo(1);
        assertThat(response.getFirst().ratingGainedTogether()).isEqualByComparingTo("0.09");
    }

    @Test
    void clubRankingsGroupPlayerActivityByClub() {
        Club topPadel = Club.builder().id(1L).name("Top Padel").city("Montevideo").integrated(true).build();
        PlayerProfile felipe = player(3L, "Felipe Rodriguez", "felipe@example.com", "5.09");
        PlayerProfile martin = player(4L, "Martin Gomez", "martin@example.com", "4.81");

        Match completedMatch = match(10L, MatchStatus.COMPLETED, Instant.now().minusSeconds(86_400), topPadel);
        MatchParticipant felipeParticipation = participant(completedMatch, felipe, MatchParticipantTeam.TEAM_ONE);

        when(playerProfileResolverService.getOrCreateByUserEmail("felipe@example.com")).thenReturn(felipe);
        when(matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(3L))
                .thenReturn(List.of(felipeParticipation));
        when(matchParticipantRepository.findAllByMatchClubIdInOrderByMatchScheduledAtDesc(java.util.Set.of(1L)))
                .thenReturn(List.of(
                        felipeParticipation,
                        participant(completedMatch, martin, MatchParticipantTeam.TEAM_ONE)
                ));

        List<PlayerClubRankingSummaryResponse> response = playerInsightService.getMyClubRankings("felipe@example.com");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().clubName()).isEqualTo("Top Padel");
        assertThat(response.getFirst().matchesPlayedByUser()).isEqualTo(1);
        assertThat(response.getFirst().competitive().userEntry().fullName()).isEqualTo("Felipe Rodriguez");
        assertThat(response.getFirst().social().topEntries()).hasSize(2);
    }

    private Match match(Long id, MatchStatus status, Instant scheduledAt, Club club) {
        return Match.builder()
                .id(id)
                .status(status)
                .scheduledAt(scheduledAt)
                .club(club)
                .locationText("Cancha 1")
                .maxPlayers(4)
                .build();
    }

    private MatchParticipant participant(Match match, PlayerProfile profile, MatchParticipantTeam team) {
        return MatchParticipant.builder()
                .match(match)
                .playerProfile(profile)
                .team(team)
                .joinedAt(Instant.now().minusSeconds(300))
                .build();
    }

    private PlayerProfile player(Long id, String fullName, String email, String rating) {
        return PlayerProfile.builder()
                .id(id)
                .user(User.builder()
                        .id(id)
                        .email(email)
                        .passwordHash("hash")
                        .role(UserRole.PLAYER)
                        .status(UserStatus.ACTIVE)
                        .build())
                .fullName(fullName)
                .currentRating(new BigDecimal(rating))
                .provisional(false)
                .matchesPlayed(10)
                .ratedMatchesCount(10)
                .surveyCompleted(true)
                .requiresClubVerification(false)
                .clubVerificationStatus(ClubVerificationStatus.NOT_REQUIRED)
                .build();
    }
}
