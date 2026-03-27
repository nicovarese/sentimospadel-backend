package com.sentimospadel.backend.shared.result;

import java.time.Duration;
import java.time.Instant;

public final class ResultEligibilityPolicy {

    public static final Duration DEFAULT_MATCH_DURATION = Duration.ofMinutes(90);

    private ResultEligibilityPolicy() {
    }

    public static Instant resultEligibleAt(Instant scheduledAt) {
        return scheduledAt == null ? null : scheduledAt.plus(DEFAULT_MATCH_DURATION);
    }

    public static boolean hasEnded(Instant scheduledAt, Instant now) {
        Instant eligibleAt = resultEligibleAt(scheduledAt);
        return eligibleAt != null && !eligibleAt.isAfter(now);
    }
}
