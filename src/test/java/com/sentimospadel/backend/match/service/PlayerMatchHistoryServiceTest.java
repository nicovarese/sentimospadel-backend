package com.sentimospadel.backend.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.player.service.PlayerProfileResolverService;
import com.sentimospadel.backend.user.entity.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerMatchHistoryServiceTest {

    @Mock
    private PlayerProfileResolverService playerProfileResolverService;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private MatchParticipantRepository matchParticipantRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @InjectMocks
    private PlayerMatchHistoryService playerMatchHistoryService;

    @Test
    void getMyMatchesReturnsOnlyAuthenticatedPlayersMatches() {
        User authenticatedUser = User.builder().id(100L).email("player@example.com").build();
        PlayerProfile authenticatedPlayer = playerProfile(10L, authenticatedUser, "Player One");
        PlayerProfile teammate = playerProfile(11L, User.builder().id(101L).email("mate@example.com").build(), "Player Two");
        PlayerProfile opponentOne = playerProfile(12L, User.builder().id(102L).email("opp1@example.com").build(), "Player Three");
        PlayerProfile opponentTwo = playerProfile(13L, User.builder().id(103L).email("opp2@example.com").build(), "Player Four");

        Match match = Match.builder()
                .id(20L)
                .createdBy(authenticatedPlayer)
                .status(MatchStatus.COMPLETED)
                .scheduledAt(Instant.parse("2026-03-17T20:00:00Z"))
                .locationText("Rambla")
                .notes("Partido cerrado")
                .maxPlayers(4)
                .build();

        MatchParticipant authenticatedParticipation = MatchParticipant.builder()
                .id(1L)
                .match(match)
                .playerProfile(authenticatedPlayer)
                .team(MatchParticipantTeam.TEAM_ONE)
                .joinedAt(Instant.parse("2026-03-17T18:00:00Z"))
                .build();

        when(playerProfileResolverService.getUserByEmail("player@example.com")).thenReturn(authenticatedUser);
        when(playerProfileRepository.findByUserId(100L)).thenReturn(Optional.of(authenticatedPlayer));
        when(matchParticipantRepository.findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(10L))
                .thenReturn(List.of(authenticatedParticipation));
        when(matchParticipantRepository.findAllByMatchIdInOrderByJoinedAtAsc(List.of(20L)))
                .thenReturn(List.of(
                        authenticatedParticipation,
                        MatchParticipant.builder().id(2L).match(match).playerProfile(teammate).team(MatchParticipantTeam.TEAM_ONE).joinedAt(Instant.parse("2026-03-17T18:01:00Z")).build(),
                        MatchParticipant.builder().id(3L).match(match).playerProfile(opponentOne).team(MatchParticipantTeam.TEAM_TWO).joinedAt(Instant.parse("2026-03-17T18:02:00Z")).build(),
                        MatchParticipant.builder().id(4L).match(match).playerProfile(opponentTwo).team(MatchParticipantTeam.TEAM_TWO).joinedAt(Instant.parse("2026-03-17T18:03:00Z")).build()
                ));
        when(matchResultRepository.findAllByMatchIdIn(List.of(20L)))
                .thenReturn(List.of(MatchResult.builder()
                        .match(match)
                        .submittedBy(authenticatedPlayer)
                        .status(MatchResultStatus.CONFIRMED)
                        .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                        .teamOneScore(2)
                        .teamTwoScore(1)
                        .submittedAt(Instant.parse("2026-03-17T21:00:00Z"))
                        .confirmedBy(opponentOne)
                        .confirmedAt(Instant.parse("2026-03-17T21:10:00Z"))
                        .build()));

        var responses = playerMatchHistoryService.getMyMatches("player@example.com");

        assertEquals(1, responses.size());
        assertEquals(20L, responses.getFirst().id());
        assertTrue(responses.getFirst().authenticatedPlayerIsParticipant());
        assertEquals(MatchParticipantTeam.TEAM_ONE, responses.getFirst().authenticatedPlayerTeam());
        assertEquals(Boolean.TRUE, responses.getFirst().authenticatedPlayerWon());
        assertEquals(4, responses.getFirst().participants().size());
        verify(matchParticipantRepository).findAllByPlayerProfileIdOrderByMatchScheduledAtDesc(10L);
    }

    @Test
    void getMyMatchesReturnsEmptyWhenAuthenticatedUserHasNoPlayerProfileYet() {
        User authenticatedUser = User.builder().id(100L).email("player@example.com").build();

        when(playerProfileResolverService.getUserByEmail("player@example.com")).thenReturn(authenticatedUser);
        when(playerProfileRepository.findByUserId(100L)).thenReturn(Optional.empty());

        var responses = playerMatchHistoryService.getMyMatches("player@example.com");

        assertTrue(responses.isEmpty());
    }

    private PlayerProfile playerProfile(Long id, User user, String fullName) {
        return PlayerProfile.builder()
                .id(id)
                .user(user)
                .fullName(fullName)
                .currentRating(new BigDecimal("4.50"))
                .provisional(false)
                .matchesPlayed(0)
                .ratedMatchesCount(0)
                .build();
    }
}
