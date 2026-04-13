package com.sentimospadel.backend.auth.ratelimit;

import com.sentimospadel.backend.auth.dto.LoginRequest;
import com.sentimospadel.backend.auth.dto.RegisterRequest;
import com.sentimospadel.backend.auth.dto.ResendEmailVerificationRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthRateLimiter {

    private static final int CLEANUP_EVERY_ATTEMPTS = 256;

    private final AuthRateLimitProperties properties;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicLong cleanupCounter = new AtomicLong();

    public void checkLogin(HttpServletRequest servletRequest, LoginRequest request) {
        check("login", request == null ? null : request.email(), servletRequest, properties.getLogin());
    }

    public void checkRegister(HttpServletRequest servletRequest, RegisterRequest request) {
        check("register", request == null ? null : request.email(), servletRequest, properties.getRegister());
    }

    public void checkResendVerification(HttpServletRequest servletRequest, ResendEmailVerificationRequest request) {
        check("resend-verification", request == null ? null : request.email(), servletRequest, properties.getResendVerification());
    }

    private void check(
            String action,
            String email,
            HttpServletRequest servletRequest,
            AuthRateLimitProperties.Rule rule
    ) {
        if (!properties.isEnabled() || rule == null || rule.getMaxAttempts() <= 0 || rule.getWindow() == null || rule.getWindow().isNegative() || rule.getWindow().isZero()) {
            return;
        }

        Instant now = Instant.now();
        cleanupExpiredCounters(now);

        String key = action + ":" + resolveClientIp(servletRequest) + ":" + normalizeEmail(email);
        Counter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || !existing.expiresAt().isAfter(now)) {
                return new Counter(new AtomicInteger(1), now.plus(rule.getWindow()));
            }
            existing.attempts().incrementAndGet();
            return existing;
        });

        if (counter.attempts().get() > rule.getMaxAttempts()) {
            long retryAfterSeconds = Math.max(1, Duration.between(now, counter.expiresAt()).toSeconds());
            throw new RateLimitExceededException("Demasiados intentos. Proba de nuevo mas tarde.", retryAfterSeconds);
        }
    }

    private void cleanupExpiredCounters(Instant now) {
        if (cleanupCounter.incrementAndGet() % CLEANUP_EVERY_ATTEMPTS != 0) {
            return;
        }
        counters.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record Counter(AtomicInteger attempts, Instant expiresAt) {
    }
}
