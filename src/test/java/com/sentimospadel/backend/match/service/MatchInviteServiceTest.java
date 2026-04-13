package com.sentimospadel.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.config.security.JwtProperties;
import com.sentimospadel.backend.club.entity.Club;
import com.sentimospadel.backend.match.config.MatchInvitationProperties;
import com.sentimospadel.backend.match.dto.MatchInviteLinkResponse;
import com.sentimospadel.backend.match.dto.MatchInvitePreviewResponse;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchInviteServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    private MatchInviteService matchInviteService;

    @BeforeEach
    void setUp() {
        matchInviteService = new MatchInviteService(
                matchRepository,
                matchParticipantRepository,
                playerProfileResolverService,
                new JwtProperties("change-this-local-jwt-secret-change-this-local-jwt-secret", 3600000L),
                new MatchInvitationProperties("http://localhost:3000", Duration.ofDays(14))
        );
    }

    @Test
    void createInviteLinkReturnsSignedShareUrlForParticipant() {
        PlayerProfile creator = playerProfile(10L, 100L, "player@example.com", "Player One");
        Match match = match(1L, creator, "Top Padel - Cancha 1");

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 10L)).thenReturn(true);

        MatchInviteLinkResponse response = matchInviteService.createInviteLink("player@example.com", 1L);

        assertThat(response.matchId()).isEqualTo(1L);
        assertThat(response.inviteToken()).isNotBlank();
        assertThat(response.inviteUrl()).contains("matchInvite=");
        assertThat(response.inviteUrl()).startsWith("http://localhost:3000");
        assertThat(response.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void createInviteLinkRejectsUserOutsideTheMatch() {
        PlayerProfile creator = playerProfile(10L, 100L, "player@example.com", "Player One");
        PlayerProfile outsider = playerProfile(11L, 101L, "other@example.com", "Other Player");
        Match match = match(1L, creator, "Top Padel - Cancha 1");

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("other@example.com")).thenReturn(outsider);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 11L)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> matchInviteService.createInviteLink("other@example.com", 1L));
    }

    @Test
    void resolveInviteReturnsPublicPreviewForSignedToken() {
        PlayerProfile creator = playerProfile(10L, 100L, "player@example.com", "Player One");
        Match match = match(1L, creator, "Top Padel - Cancha 1");

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));
        when(playerProfileResolverService.getOrCreateByUserEmail("player@example.com")).thenReturn(creator);
        when(matchParticipantRepository.existsByMatchIdAndPlayerProfileId(1L, 10L)).thenReturn(true);
        when(matchParticipantRepository.countByMatchId(1L)).thenReturn(2L);

        MatchInviteLinkResponse link = matchInviteService.createInviteLink("player@example.com", 1L);
        MatchInvitePreviewResponse preview = matchInviteService.resolveInvite(link.inviteToken());

        assertThat(preview.matchId()).isEqualTo(1L);
        assertThat(preview.clubName()).isEqualTo("Top Padel");
        assertThat(preview.courtName()).isEqualTo("Cancha 1");
        assertThat(preview.currentPlayerCount()).isEqualTo(2);
        assertThat(preview.createdByName()).isEqualTo("Player One");
    }

    @Test
    void resolveInviteRejectsMalformedToken() {
        assertThrows(BadRequestException.class, () -> matchInviteService.resolveInvite("invalid-token"));
    }

    private Match match(Long id, PlayerProfile creator, String locationText) {
        Club club = Club.builder().name("Top Padel").city("Montevideo").build();
        ReflectionTestUtils.setField(club, "id", 7L);
        Match match = Match.builder()
                .createdBy(creator)
                .status(MatchStatus.OPEN)
                .scheduledAt(Instant.parse("2026-04-14T20:00:00Z"))
                .club(club)
                .locationText(locationText)
                .notes("Por los puntos")
                .maxPlayers(4)
                .build();
        ReflectionTestUtils.setField(match, "id", id);
        return match;
    }

    private PlayerProfile playerProfile(Long profileId, Long userId, String email, String fullName) {
        return PlayerProfile.builder()
                .id(profileId)
                .user(User.builder()
                        .id(userId)
                        .email(email)
                        .phone("091234567")
                        .passwordHash("hash")
                        .role(UserRole.PLAYER)
                        .status(UserStatus.ACTIVE)
                        .build())
                .fullName(fullName)
                .currentRating(BigDecimal.valueOf(4.50))
                .provisional(false)
                .matchesPlayed(10)
                .ratedMatchesCount(10)
                .surveyCompleted(true)
                .requiresClubVerification(false)
                .clubVerificationStatus(com.sentimospadel.backend.player.enums.ClubVerificationStatus.NOT_REQUIRED)
                .build();
    }
}
