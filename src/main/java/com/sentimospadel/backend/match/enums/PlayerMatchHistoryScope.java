package com.sentimospadel.backend.match.enums;

import com.sentimospadel.backend.match.entity.Match;
import com.sentimospadel.backend.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.Arrays;

public enum PlayerMatchHistoryScope {
    UPCOMING("upcoming"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    PENDING_RESULT("pending_result");

    private final String queryValue;

    PlayerMatchHistoryScope(String queryValue) {
        this.queryValue = queryValue;
    }

    public static PlayerMatchHistoryScope fromQuery(String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(value -> value.queryValue.equalsIgnoreCase(scope))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Invalid match scope. Supported values: upcoming, completed, cancelled, pending_result"
                ));
    }

    public boolean includes(Match match, Instant now) {
        return switch (this) {
            case UPCOMING -> match.getStatus() != MatchStatus.CANCELLED
                    && match.getStatus() != MatchStatus.COMPLETED
                    && match.getScheduledAt().isAfter(now);
            case COMPLETED -> match.getStatus() == MatchStatus.COMPLETED;
            case CANCELLED -> match.getStatus() == MatchStatus.CANCELLED;
            case PENDING_RESULT -> match.getStatus() == MatchStatus.RESULT_PENDING;
        };
    }
}
