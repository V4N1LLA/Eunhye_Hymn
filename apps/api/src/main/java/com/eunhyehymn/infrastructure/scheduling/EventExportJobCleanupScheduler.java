package com.eunhyehymn.infrastructure.scheduling;

import com.eunhyehymn.application.usecases.CleanupEventExportJobsUseCase;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventExportJobCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(EventExportJobCleanupScheduler.class);

    private final CleanupEventExportJobsUseCase cleanupEventExportJobsUseCase;

    public EventExportJobCleanupScheduler(CleanupEventExportJobsUseCase cleanupEventExportJobsUseCase) {
        this.cleanupEventExportJobsUseCase = cleanupEventExportJobsUseCase;
    }

    @Scheduled(
        cron = "${events.export.jobs.cleanup-cron:0 15 3 * * *}",
        zone = "${events.export.jobs.cleanup-zone:UTC}"
    )
    public void cleanupFinishedJobs() {
        CleanupEventExportJobsUseCase.Result result = cleanupEventExportJobsUseCase.cleanup(Instant.now());
        logger.info(
            "event-export cleanup completed: deleted={}, retentionDays={}, completedBeforeExclusive={}",
            result.deletedCount(),
            result.retentionDays(),
            result.completedBeforeExclusive()
        );
    }
}
