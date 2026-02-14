package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.repository.EventExportJobRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CleanupEventExportJobsUseCase {
    private static final int MIN_RETENTION_DAYS = 1;

    private final EventExportJobRepository eventExportJobRepository;
    private final int retentionDays;

    public CleanupEventExportJobsUseCase(
        EventExportJobRepository eventExportJobRepository,
        int retentionDays
    ) {
        this.eventExportJobRepository = eventExportJobRepository;
        this.retentionDays = normalizeRetentionDays(retentionDays);
    }

    public Result cleanup(Instant now) {
        Instant completedBeforeExclusive = now.minus(retentionDays, ChronoUnit.DAYS);
        long deleted = eventExportJobRepository.deleteCompletedOrFailedBefore(completedBeforeExclusive);
        return new Result(retentionDays, completedBeforeExclusive, deleted);
    }

    private int normalizeRetentionDays(int value) {
        if (value < MIN_RETENTION_DAYS) {
            return MIN_RETENTION_DAYS;
        }
        return value;
    }

    public record Result(
        int retentionDays,
        Instant completedBeforeExclusive,
        long deletedCount
    ) {
    }
}
