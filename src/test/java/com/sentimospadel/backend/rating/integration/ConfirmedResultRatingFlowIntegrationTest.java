package com.sentimospadel.backend.rating.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sentimospadel.backend.BackendApplication;
import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.match.entity.MatchParticipant;
import com.sentimospadel.backend.match.entity.MatchResult;
import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import com.sentimospadel.backend.match.enums.MatchResultStatus;
import com.sentimospadel.backend.match.enums.MatchStatus;
import com.sentimospadel.backend.match.enums.MatchWinnerTeam;
import com.sentimospadel.backend.match.repository.MatchParticipantRepository;
import com.sentimospadel.backend.match.repository.MatchRepository;
import com.sentimospadel.backend.match.repository.MatchResultRepository;
import com.sentimospadel.backend.match.service.MatchService;
import com.sentimospadel.backend.player.entity.PlayerProfile;
import com.sentimospadel.backend.player.enums.ClubVerificationStatus;
import com.sentimospadel.backend.player.repository.PlayerProfileRepository;
import com.sentimospadel.backend.rating.repository.PlayerRatingHistoryRepository;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.enums.UserRole;
import com.sentimospadel.backend.user.enums.UserStatus;
import com.sentimospadel.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = BackendApplication.class)
@ActiveProfiles("test")
@Transactional
class ConfirmedResultRatingFlowIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchParticipantRepository matchParticipantRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    @Autowired
    private PlayerRatingHistoryRepository playerRatingHistoryRepository;

    @Test
    void flywayLoadsRatingSchema() {
        Integer historyTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) = 'player_rating_history'",
                Integer.class
        );
        Integer currentRatingColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE lower(table_name) = 'player_profiles' AND lower(column_name) = 'current_rating'",
                Integer.class
        );

        assertEquals(1, historyTableCount);
        assertEquals(1, currentRatingColumnCount);
    }

    @Test
    void confirmedResultUpdatesRatingsAndCreatesHistoryExactlyOnce() {
        PlayerProfile creator = createPlayer("teamone.one@example.com", "Team One One", "4.80");
        PlayerProfile teammate = createPlayer("teamone.two@example.com", "Team One Two", "4.60");
        PlayerProfile confirmer = createPlayer("teamtwo.one@example.com", "Team Two One", "4.90");
        PlayerProfile opponent = createPlayer("teamtwo.two@example.com", "Team Two Two", "4.70");

        Match match = matchRepository.save(Match.builder()
                .createdBy(creator)
                .status(MatchStatus.RESULT_PENDING)
                .scheduledAt(Instant.parse("2026-03-20T20:00:00Z"))
                .maxPlayers(4)
                .locationText("Rambla")
                .notes("Integration test")
                .build());

        matchParticipantRepository.saveAll(java.util.List.of(
                participant(match, creator, MatchParticipantTeam.TEAM_ONE),
                participant(match, teammate, MatchParticipantTeam.TEAM_ONE),
                participant(match, confirmer, MatchParticipantTeam.TEAM_TWO),
                participant(match, opponent, MatchParticipantTeam.TEAM_TWO)
        ));

        matchResultRepository.save(MatchResult.builder()
                .match(match)
                .submittedBy(creator)
                .status(MatchResultStatus.PENDING)
                .winnerTeam(MatchWinnerTeam.TEAM_ONE)
                .teamOneScore(2)
                .teamTwoScore(1)
                .submittedAt(Instant.parse("2026-03-17T17:00:00Z"))
                .build());

        BigDecimal creatorOldRating = creator.getCurrentRating();
        BigDecimal confirmerOldRating = confirmer.getCurrentRating();

        matchService.confirmResult("teamtwo.one@example.com", match.getId());

        MatchResult confirmedResult = matchResultRepository.findByMatchId(match.getId()).orElseThrow();
        PlayerProfile creatorAfter = playerProfileRepository.findById(creator.getId()).orElseThrow();
        PlayerProfile confirmerAfter = playerProfileRepository.findById(confirmer.getId()).orElseThrow();

        assertEquals(MatchResultStatus.CONFIRMED, confirmedResult.getStatus());
        assertTrue(confirmedResult.isRatingApplied());
        assertEquals(4L, playerRatingHistoryRepository.countByMatchId(match.getId()));
        assertNotEquals(creatorOldRating, creatorAfter.getCurrentRating());
        assertNotEquals(confirmerOldRating, confirmerAfter.getCurrentRating());
        assertEquals(1, creatorAfter.getRatedMatchesCount());
        assertEquals(1, confirmerAfter.getRatedMatchesCount());

        BigDecimal creatorRatingAfterFirstApply = creatorAfter.getCurrentRating();
        BigDecimal confirmerRatingAfterFirstApply = confirmerAfter.getCurrentRating();

        matchService.confirmResult("teamtwo.one@example.com", match.getId());

        PlayerProfile creatorAfterSecondConfirm = playerProfileRepository.findById(creator.getId()).orElseThrow();
        PlayerProfile confirmerAfterSecondConfirm = playerProfileRepository.findById(confirmer.getId()).orElseThrow();

        assertEquals(4L, playerRatingHistoryRepository.countByMatchId(match.getId()));
        assertEquals(creatorRatingAfterFirstApply, creatorAfterSecondConfirm.getCurrentRating());
        assertEquals(confirmerRatingAfterFirstApply, confirmerAfterSecondConfirm.getCurrentRating());
        assertEquals(1, creatorAfterSecondConfirm.getRatedMatchesCount());
        assertEquals(1, confirmerAfterSecondConfirm.getRatedMatchesCount());
    }

    private PlayerProfile createPlayer(String email, String fullName, String rating) {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("hash")
                .role(UserRole.PLAYER)
                .status(UserStatus.ACTIVE)
                .build());

        return playerProfileRepository.save(PlayerProfile.builder()
                .user(user)
                .fullName(fullName)
                .currentRating(new BigDecimal(rating))
                .provisional(true)
                .matchesPlayed(0)
                .ratedMatchesCount(0)
                .surveyCompleted(false)
                .requiresClubVerification(false)
                .clubVerificationStatus(ClubVerificationStatus.NOT_REQUIRED)
                .build());
    }

    private MatchParticipant participant(Match match, PlayerProfile playerProfile, MatchParticipantTeam team) {
        return MatchParticipant.builder()
                .match(match)
                .playerProfile(playerProfile)
                .team(team)
                .joinedAt(Instant.parse("2026-03-17T16:00:00Z"))
                .build();
    }
}
