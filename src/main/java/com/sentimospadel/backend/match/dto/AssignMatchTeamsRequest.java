package com.sentimospadel.backend.match.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignMatchTeamsRequest(
        @NotEmpty @Valid List<MatchTeamAssignmentRequest> assignments
) {
}
