package com.eunhyehymn.infrastructure.scheduling;

import com.eunhyehymn.application.usecases.GetEventExportOpsMetricsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventExportJobOpsAlertScheduler {
    private static final Logger logger = LoggerFactory.getLogger(EventExportJobOpsAlertScheduler.class);

    private final GetEventExportOpsMetricsUseCase getEventExportOpsMetricsUseCase;
    private final int windowDays;
    private final int minFinishedJobs;
    private final int maxQueuedJobs;
    private final double maxFailureRatePercent;
    private final double maxP95Seconds;

    public EventExportJobOpsAlertScheduler(
        GetEventExportOpsMetricsUseCase getEventExportOpsMetricsUseCase,
        @Value("${events.export.jobs.alert.window-days:1}") int windowDays,
        @Value("${events.export.jobs.alert.min-finished-jobs:5}") int minFinishedJobs,
        @Value("${events.export.jobs.alert.max-queued-jobs:20}") int maxQueuedJobs,
        @Value("${events.export.jobs.alert.max-failure-rate-percent:20}") double maxFailureRatePercent,
        @Value("${events.export.jobs.alert.max-p95-seconds:120}") double maxP95Seconds
    ) {
        this.getEventExportOpsMetricsUseCase = getEventExportOpsMetricsUseCase;
        this.windowDays = normalizePositive(windowDays, 1);
        this.minFinishedJobs = normalizePositive(minFinishedJobs, 5);
        this.maxQueuedJobs = normalizePositive(maxQueuedJobs, 20);
        this.maxFailureRatePercent = Math.max(maxFailureRatePercent, 0.0);
        this.maxP95Seconds = Math.max(maxP95Seconds, 1.0);
    }

    @Scheduled(
        cron = "${events.export.jobs.alert.cron:0 */10 * * * *}",
        zone = "${events.export.jobs.alert.zone:UTC}"
    )
    public void evaluateOpsAlerts() {
        GetEventExportOpsMetricsUseCase.Result metrics = getEventExportOpsMetricsUseCase.execute(windowDays);
        long finishedJobs = metrics.jobs().completed() + metrics.jobs().failed();

        if (finishedJobs >= minFinishedJobs && metrics.jobs().failureRatePercent() > maxFailureRatePercent) {
            logger.warn(
                "event-export alert: failureRatePercent={} exceeds threshold={} (windowDays={}, finishedJobs={})",
                metrics.jobs().failureRatePercent(),
                maxFailureRatePercent,
                metrics.windowDays(),
                finishedJobs
            );
        }

        if (metrics.processing().measuredJobs() >= minFinishedJobs && metrics.processing().p95Seconds() > maxP95Seconds) {
            logger.warn(
                "event-export alert: p95Seconds={} exceeds threshold={} (windowDays={}, measuredJobs={})",
                metrics.processing().p95Seconds(),
                maxP95Seconds,
                metrics.windowDays(),
                metrics.processing().measuredJobs()
            );
        }

        if (metrics.jobs().queued() > maxQueuedJobs) {
            logger.warn(
                "event-export alert: queuedJobs={} exceeds threshold={} (windowDays={})",
                metrics.jobs().queued(),
                maxQueuedJobs,
                metrics.windowDays()
            );
        }
    }

    private int normalizePositive(int value, int fallback) {
        if (value < 1) {
            return fallback;
        }
        return value;
    }
}
