package com.eunhyehymn.infrastructure.scheduling;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuthAiOpsAlertScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AuthAiOpsAlertScheduler.class);
    private static final List<String> DEFAULT_AUTH_SCOPES = List.of(
        "admin_login",
        "social_login",
        "user_signup",
        "user_login",
        "invite_validate"
    );

    private final MeterRegistry meterRegistry;
    private final boolean enabled;
    private final int minAuthRequests;
    private final int minAiRequests;
    private final double maxAuthFailureRatePercent;
    private final double maxAiFailureRatePercent;
    private final double maxAiFallbackRatePercent;
    private final List<String> authScopes;

    public AuthAiOpsAlertScheduler(
        MeterRegistry meterRegistry,
        @Value("${ops.auth-ai-alert.enabled:true}") boolean enabled,
        @Value("${ops.auth-ai-alert.min-auth-requests:100}") int minAuthRequests,
        @Value("${ops.auth-ai-alert.min-ai-requests:30}") int minAiRequests,
        @Value("${ops.auth-ai-alert.max-auth-failure-rate-percent:5}") double maxAuthFailureRatePercent,
        @Value("${ops.auth-ai-alert.max-ai-failure-rate-percent:1}") double maxAiFailureRatePercent,
        @Value("${ops.auth-ai-alert.max-ai-fallback-rate-percent:5}") double maxAiFallbackRatePercent,
        @Value("${ops.auth-ai-alert.auth-scopes:admin_login,social_login,user_signup,user_login,invite_validate}") String authScopesRaw
    ) {
        this.meterRegistry = meterRegistry;
        this.enabled = enabled;
        this.minAuthRequests = normalizePositive(minAuthRequests, 100);
        this.minAiRequests = normalizePositive(minAiRequests, 30);
        this.maxAuthFailureRatePercent = normalizeNonNegative(maxAuthFailureRatePercent, 5.0);
        this.maxAiFailureRatePercent = normalizeNonNegative(maxAiFailureRatePercent, 1.0);
        this.maxAiFallbackRatePercent = normalizeNonNegative(maxAiFallbackRatePercent, 5.0);
        this.authScopes = parseAuthScopes(authScopesRaw);
    }

    @Scheduled(
        cron = "${ops.auth-ai-alert.cron:0 */10 * * * *}",
        zone = "${ops.auth-ai-alert.zone:UTC}"
    )
    public void evaluateOpsAlerts() {
        if (!enabled) {
            return;
        }

        OpsSnapshot snapshot = evaluateSnapshot();

        if (snapshot.authFailure().breached()) {
            logger.warn(
                "auth ops alert: failureRatePercent={} exceeds threshold={} (failed={}, total={}, minSamples={}, scopes={})",
                snapshot.authFailure().ratePercent(),
                snapshot.authFailure().thresholdPercent(),
                snapshot.authFailure().numeratorCount(),
                snapshot.authFailure().denominatorCount(),
                snapshot.authFailure().minSampleCount(),
                String.join(",", authScopes)
            );
        }

        if (snapshot.aiFailure().breached()) {
            logger.warn(
                "ai ops alert: failureRatePercent={} exceeds threshold={} (failed={}, total={}, minSamples={})",
                snapshot.aiFailure().ratePercent(),
                snapshot.aiFailure().thresholdPercent(),
                snapshot.aiFailure().numeratorCount(),
                snapshot.aiFailure().denominatorCount(),
                snapshot.aiFailure().minSampleCount()
            );
        }

        if (snapshot.aiFallback().breached()) {
            logger.warn(
                "ai ops alert: fallbackRatePercent={} exceeds threshold={} (fallback={}, total={}, minSamples={})",
                snapshot.aiFallback().ratePercent(),
                snapshot.aiFallback().thresholdPercent(),
                snapshot.aiFallback().numeratorCount(),
                snapshot.aiFallback().denominatorCount(),
                snapshot.aiFallback().minSampleCount()
            );
        }
    }

    OpsSnapshot evaluateSnapshot() {
        double authSuccessCount = 0.0;
        double authFailureCount = 0.0;
        for (String scope : authScopes) {
            authSuccessCount += counterValue("auth_requests_total", "scope", scope, "result", "success");
            authFailureCount += counterValue("auth_requests_total", "scope", scope, "result", "failure");
        }
        double authTotal = authSuccessCount + authFailureCount;
        double authFailureRatePercent = toRatePercent(authFailureCount, authTotal);

        double aiSuccessCount = counterValue("ai_recommend_requests_total", "result", "success");
        double aiTotalCount = sumCounters("ai_recommend_requests_total");
        double aiFailureCount = Math.max(0.0, aiTotalCount - aiSuccessCount);
        double aiFailureRatePercent = toRatePercent(aiFailureCount, aiTotalCount);
        double aiFallbackCount = sumCounters("ai_recommend_fallback_total");
        double aiFallbackRatePercent = toRatePercent(aiFallbackCount, aiTotalCount);

        return new OpsSnapshot(
            authSuccessCount,
            authFailureCount,
            aiSuccessCount,
            aiFailureCount,
            aiFallbackCount,
            new ThresholdCheck(
                authFailureCount,
                authTotal,
                authFailureRatePercent,
                maxAuthFailureRatePercent,
                minAuthRequests,
                authTotal >= minAuthRequests,
                authTotal >= minAuthRequests && authFailureRatePercent > maxAuthFailureRatePercent
            ),
            new ThresholdCheck(
                aiFailureCount,
                aiTotalCount,
                aiFailureRatePercent,
                maxAiFailureRatePercent,
                minAiRequests,
                aiTotalCount >= minAiRequests,
                aiTotalCount >= minAiRequests && aiFailureRatePercent > maxAiFailureRatePercent
            ),
            new ThresholdCheck(
                aiFallbackCount,
                aiTotalCount,
                aiFallbackRatePercent,
                maxAiFallbackRatePercent,
                minAiRequests,
                aiTotalCount >= minAiRequests,
                aiTotalCount >= minAiRequests && aiFallbackRatePercent > maxAiFallbackRatePercent
            )
        );
    }

    private double counterValue(String meterName, String... tags) {
        Counter counter = meterRegistry.find(meterName).tags(tags).counter();
        if (counter == null) {
            return 0.0;
        }
        return counter.count();
    }

    private double sumCounters(String meterName) {
        return meterRegistry.find(meterName)
            .counters()
            .stream()
            .mapToDouble(Counter::count)
            .sum();
    }

    private static int normalizePositive(int value, int fallback) {
        if (value < 1) {
            return fallback;
        }
        return value;
    }

    private static double normalizeNonNegative(double value, double fallback) {
        if (value < 0.0) {
            return fallback;
        }
        return value;
    }

    private static List<String> parseAuthScopes(String raw) {
        List<String> parsed = Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(scope -> !scope.isBlank())
            .map(scope -> scope.toLowerCase(Locale.ROOT))
            .distinct()
            .toList();
        if (parsed.isEmpty()) {
            return DEFAULT_AUTH_SCOPES;
        }
        return parsed;
    }

    private static double toRatePercent(double numerator, double denominator) {
        if (denominator <= 0.0) {
            return 0.0;
        }
        return (numerator * 100.0) / denominator;
    }

    public record OpsSnapshot(
        double authSuccessCount,
        double authFailureCount,
        double aiSuccessCount,
        double aiFailureCount,
        double aiFallbackCount,
        ThresholdCheck authFailure,
        ThresholdCheck aiFailure,
        ThresholdCheck aiFallback
    ) {
        public OpsSnapshot {
            Objects.requireNonNull(authFailure, "authFailure");
            Objects.requireNonNull(aiFailure, "aiFailure");
            Objects.requireNonNull(aiFallback, "aiFallback");
        }
    }

    public record ThresholdCheck(
        double numeratorCount,
        double denominatorCount,
        double ratePercent,
        double thresholdPercent,
        int minSampleCount,
        boolean sampleReady,
        boolean breached
    ) {
    }
}
