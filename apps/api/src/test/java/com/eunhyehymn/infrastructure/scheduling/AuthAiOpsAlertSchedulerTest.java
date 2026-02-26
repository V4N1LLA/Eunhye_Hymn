package com.eunhyehymn.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class AuthAiOpsAlertSchedulerTest {
    @Test
    void evaluateSnapshotAggregatesOnlyConfiguredAuthScopes() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.counter("auth_requests_total", "scope", "user_login", "result", "success").increment(80.0);
        meterRegistry.counter("auth_requests_total", "scope", "user_login", "result", "failure").increment(20.0);
        meterRegistry.counter("auth_requests_total", "scope", "social_login", "result", "success").increment(5.0);
        meterRegistry.counter("auth_requests_total", "scope", "social_login", "result", "failure").increment(5.0);
        meterRegistry.counter("auth_requests_total", "scope", "admin_login", "result", "failure").increment(30.0);

        AuthAiOpsAlertScheduler scheduler = new AuthAiOpsAlertScheduler(
            meterRegistry,
            true,
            50,
            30,
            15.0,
            10.0,
            10.0,
            "user_login,social_login"
        );

        AuthAiOpsAlertScheduler.OpsSnapshot snapshot = scheduler.evaluateSnapshot();

        assertThat(snapshot.authSuccessCount()).isEqualTo(85.0);
        assertThat(snapshot.authFailureCount()).isEqualTo(25.0);
        assertThat(snapshot.authFailure().denominatorCount()).isEqualTo(110.0);
        assertThat(snapshot.authFailure().ratePercent()).isCloseTo(22.7272, offset(0.0001));
        assertThat(snapshot.authFailure().sampleReady()).isTrue();
        assertThat(snapshot.authFailure().breached()).isTrue();
    }

    @Test
    void evaluateSnapshotFlagsAiFailureAndFallbackThresholdBreaches() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.counter("ai_recommend_requests_total", "result", "success").increment(60.0);
        meterRegistry.counter("ai_recommend_requests_total", "result", "validation_error").increment(20.0);
        meterRegistry.counter("ai_recommend_requests_total", "result", "ai_unavailable").increment(20.0);
        meterRegistry.counter("ai_recommend_fallback_total", "result", "success").increment(15.0);

        AuthAiOpsAlertScheduler scheduler = new AuthAiOpsAlertScheduler(
            meterRegistry,
            true,
            100,
            30,
            10.0,
            10.0,
            10.0,
            "user_login"
        );

        AuthAiOpsAlertScheduler.OpsSnapshot snapshot = scheduler.evaluateSnapshot();

        assertThat(snapshot.aiSuccessCount()).isEqualTo(60.0);
        assertThat(snapshot.aiFailureCount()).isEqualTo(40.0);
        assertThat(snapshot.aiFallbackCount()).isEqualTo(15.0);
        assertThat(snapshot.aiFailure().ratePercent()).isEqualTo(40.0);
        assertThat(snapshot.aiFailure().sampleReady()).isTrue();
        assertThat(snapshot.aiFailure().breached()).isTrue();
        assertThat(snapshot.aiFallback().ratePercent()).isEqualTo(15.0);
        assertThat(snapshot.aiFallback().sampleReady()).isTrue();
        assertThat(snapshot.aiFallback().breached()).isTrue();
    }

    @Test
    void evaluateSnapshotDoesNotBreachBeforeMinimumSamples() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.counter("auth_requests_total", "scope", "user_login", "result", "success").increment(3.0);
        meterRegistry.counter("auth_requests_total", "scope", "user_login", "result", "failure").increment(1.0);
        meterRegistry.counter("ai_recommend_requests_total", "result", "success").increment(4.0);
        meterRegistry.counter("ai_recommend_requests_total", "result", "error").increment(1.0);
        meterRegistry.counter("ai_recommend_fallback_total", "result", "success").increment(2.0);

        AuthAiOpsAlertScheduler scheduler = new AuthAiOpsAlertScheduler(
            meterRegistry,
            true,
            10,
            10,
            0.0,
            0.0,
            0.0,
            "user_login"
        );

        AuthAiOpsAlertScheduler.OpsSnapshot snapshot = scheduler.evaluateSnapshot();

        assertThat(snapshot.authFailure().sampleReady()).isFalse();
        assertThat(snapshot.authFailure().breached()).isFalse();
        assertThat(snapshot.aiFailure().sampleReady()).isFalse();
        assertThat(snapshot.aiFailure().breached()).isFalse();
        assertThat(snapshot.aiFallback().sampleReady()).isFalse();
        assertThat(snapshot.aiFallback().breached()).isFalse();
    }
}
