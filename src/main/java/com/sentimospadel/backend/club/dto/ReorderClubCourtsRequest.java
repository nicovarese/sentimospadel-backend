package com.sentimospadel.backend.club.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderClubCourtsRequest(
        @NotEmpty List<@NotNull Long> orderedCourtIds
) {
}
