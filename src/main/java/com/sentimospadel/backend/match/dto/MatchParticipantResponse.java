package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import java.time.Instant;

public record MatchParticipantResponse(
        Long playerProfileId,
        Long userId,
        String fullName,
        MatchParticipantTeam team,
        Instant joinedAt
) {
}
