package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.EventExportJobStatus;
import com.eunhyehymn.domain.repository.EventExportJobCleanupRunRepository;
import com.eunhyehymn.domain.repository.EventExportJobRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class GetEventExportOpsMetricsUseCase {
    private static final int DEFAULT_WINDOW_DAYS = 7;
    private static final int MIN_WINDOW_DAYS = 1;
    private static final int MAX_WINDOW_DAYS = 90;

    private final EventExportJobRepository eventExportJobRepository;
    private final EventExportJobCleanupRunRepository cleanupRunRepository;

    public GetEventExportOpsMetricsUseCase(
        EventExportJobRepository eventExportJobRepository,
        EventExportJobCleanupRunRepository cleanupRunRepository
    ) {
        this.eventExportJobRepository = eventExportJobRepository;
        this.cleanupRunRepository = cleanupRunRepository;
    }

    public Result execute(Integer windowDays) {
        return execute(windowDays, Instant.now());
    }

    Result execute(Integer windowDays, Instant now) {
        int normalizedWindowDays = normalizeWindowDays(windowDays);
        Instant toExclusive = now;
        Instant fromInclusive = toExclusive.minus(normalizedWindowDays, ChronoUnit.DAYS);

        List<EventExportJobRepository.MetricsRow> rows = eventExportJobRepository.findMetricsRows(fromInclusive, toExclusive);
        JobMetrics jobMetrics = summarizeJobs(rows);
        ProcessingMetrics processingMetrics = summarizeProcessing(rows);
        EventExportJobCleanupRunRepository.Summary cleanupSummary = cleanupRunRepository.summarize(fromInclusive, toExclusive);

        return new Result(
            normalizedWindowDays,
            fromInclusive,
            toExclusive,
            jobMetrics,
            processingMetrics,
            new CleanupMetrics(cleanupSummary.runCount(), cleanupSummary.deletedJobCount())
        );
    }

    private int normalizeWindowDays(Integer windowDays) {
        if (windowDays == null) {
            return DEFAULT_WINDOW_DAYS;
        }
        if (windowDays < MIN_WINDOW_DAYS) {
            return MIN_WINDOW_DAYS;
        }
        return Math.min(windowDays, MAX_WINDOW_DAYS);
    }

    private JobMetrics summarizeJobs(List<EventExportJobRepository.MetricsRow> rows) {
        long queued = 0;
        long running = 0;
        long completed = 0;
        long failed = 0;
        for (EventExportJobRepository.MetricsRow row : rows) {
            EventExportJobStatus status = row.status();
            if (status == EventExportJobStatus.QUEUED) {
                queued += 1;
            } else if (status == EventExportJobStatus.RUNNING) {
                running += 1;
            } else if (status == EventExportJobStatus.COMPLETED) {
                completed += 1;
            } else if (status == EventExportJobStatus.FAILED) {
                failed += 1;
            }
        }
        long finished = completed + failed;
        double failureRatePercent = finished == 0 ? 0.0 : roundTo2((failed * 100.0) / finished);

        return new JobMetrics(rows.size(), queued, running, completed, failed, failureRatePercent);
    }

    private ProcessingMetrics summarizeProcessing(List<EventExportJobRepository.MetricsRow> rows) {
        List<Long> durationsSeconds = new ArrayList<>();
        for (EventExportJobRepository.MetricsRow row : rows) {
            if (row.startedAt() == null || row.completedAt() == null) {
                continue;
            }
            long seconds = Duration.between(row.startedAt(), row.completedAt()).toSeconds();
            if (seconds < 0) {
                continue;
            }
            durationsSeconds.add(seconds);
        }

        if (durationsSeconds.isEmpty()) {
            return new ProcessingMetrics(0, 0.0, 0.0);
        }

        durationsSeconds.sort(Long::compareTo);
        double sumSeconds = durationsSeconds.stream().mapToLong(Long::longValue).sum();
        double averageSeconds = roundTo2(sumSeconds / durationsSeconds.size());
        double p95Seconds = roundTo2(percentile(durationsSeconds, 0.95));

        return new ProcessingMetrics(durationsSeconds.size(), averageSeconds, p95Seconds);
    }

    private double percentile(List<Long> sortedValues, double percentile) {
        int rank = (int) Math.ceil(percentile * sortedValues.size());
        int index = Math.max(rank - 1, 0);
        return sortedValues.get(index);
    }

    private double roundTo2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record Result(
        int windowDays,
        Instant fromInclusive,
        Instant toExclusive,
        JobMetrics jobs,
        ProcessingMetrics processing,
        CleanupMetrics cleanup
    ) {
    }

    public record JobMetrics(
        long total,
        long queued,
        long running,
        long completed,
        long failed,
        double failureRatePercent
    ) {
    }

    public record ProcessingMetrics(
        int measuredJobs,
        double averageSeconds,
        double p95Seconds
    ) {
    }

    public record CleanupMetrics(
        long runCount,
        long deletedJobs
    ) {
    }
}
