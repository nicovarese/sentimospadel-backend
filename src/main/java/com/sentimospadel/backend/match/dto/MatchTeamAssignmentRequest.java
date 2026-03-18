package com.sentimospadel.backend.match.dto;

import com.sentimospadel.backend.match.enums.MatchParticipantTeam;
import jakarta.validation.constraints.NotNull;

public record MatchTeamAssignmentRequest(
        @NotNull Long playerProfileId,
        @NotNull MatchParticipantTeam team
) {
}
