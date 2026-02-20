package com.eunhyehymn.infrastructure.security;

import com.eunhyehymn.common.error.ApiException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimitService {
    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitService.class);
    private static final String RATE_LIMIT_MESSAGE = "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";

    private final MeterRegistry meterRegistry;
    private final boolean enabled;
    private final int windowSeconds;
    private final int maxAttemptsPerKey;
    private final Clock clock;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong callCounter = new AtomicLong(0);

    public AuthRateLimitService(
        MeterRegistry meterRegistry,
        @Value("${security.rate-limit.auth.enabled:true}") boolean enabled,
        @Value("${security.rate-limit.auth.window-seconds:60}") int windowSeconds,
        @Value("${security.rate-limit.auth.max-attempts-per-key:25}") int maxAttemptsPerKey
    ) {
        this.meterRegistry = meterRegistry;
        this.enabled = enabled;
        this.windowSeconds = Math.max(1, windowSeconds);
        this.maxAttemptsPerKey = Math.max(1, maxAttemptsPerKey);
        this.clock = Clock.systemUTC();
    }

    public void checkOrThrow(String scope, String subject) {
        if (!enabled) {
            return;
        }

        String normalizedScope = normalizeScope(scope);
        String normalizedSubject = normalizeSubject(subject);
        boolean allowed = tryAcquire(normalizedScope, normalizedSubject);
        if (allowed) {
            meterRegistry.counter("auth_rate_limit_allowed_total", "scope", normalizedScope).increment();
            return;
        }

        meterRegistry.counter("auth_rate_limit_rejected_total", "scope", normalizedScope).increment();
        log.warn(
            "auth rate limit exceeded: scope={} subject={} windowSeconds={} maxAttemptsPerKey={}",
            normalizedScope,
            normalizedSubject,
            windowSeconds,
            maxAttemptsPerKey
        );

        throw new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            "too_many_requests",
            RATE_LIMIT_MESSAGE,
            Map.of("scope", normalizedScope, "windowSeconds", windowSeconds)
        );
    }

    public void recordOutcome(String scope, boolean success) {
        meterRegistry.counter(
            "auth_requests_total",
            "scope",
            normalizeScope(scope),
            "result",
            success ? "success" : "failure"
        ).increment();
    }

    private boolean tryAcquire(String scope, String subject) {
        long nowEpochSecond = Instant.now(clock).getEpochSecond();
        long windowStart = (nowEpochSecond / windowSeconds) * windowSeconds;
        String key = scope + ":" + subject;

        AtomicBoolean allowed = new AtomicBoolean(false);
        counters.compute(key, (ignored, existing) -> {
            WindowCounter next = existing;
            if (next == null || next.windowStartEpochSecond != windowStart) {
                next = new WindowCounter(windowStart, 0, nowEpochSecond);
            }
            next.lastSeenEpochSecond = nowEpochSecond;
            if (next.count < maxAttemptsPerKey) {
                next.count += 1;
                allowed.set(true);
            }
            return next;
        });

        maybeCleanup(nowEpochSecond);
        return allowed.get();
    }

    private void maybeCleanup(long nowEpochSecond) {
        long currentCalls = callCounter.incrementAndGet();
        if (currentCalls % 256 != 0) {
            return;
        }

        long expireBefore = nowEpochSecond - (windowSeconds * 2L);
        counters.entrySet().removeIf(entry -> entry.getValue().lastSeenEpochSecond < expireBefore);
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "unknown";
        }
        return scope.trim().toLowerCase();
    }

    private String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return "anonymous";
        }
        String normalized = subject.trim().toLowerCase();
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120);
    }

    private static final class WindowCounter {
        long windowStartEpochSecond;
        int count;
        long lastSeenEpochSecond;

        private WindowCounter(long windowStartEpochSecond, int count, long lastSeenEpochSecond) {
            this.windowStartEpochSecond = windowStartEpochSecond;
            this.count = count;
            this.lastSeenEpochSecond = lastSeenEpochSecond;
        }
    }
}
