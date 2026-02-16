package com.eunhyehymn.infrastructure.scheduling;

import com.eunhyehymn.application.usecases.CleanupEventExportJobsUseCase;
import com.eunhyehymn.domain.model.EventExportJobCleanupRun;
import com.eunhyehymn.domain.repository.EventExportJobCleanupRunRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventExportJobCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(EventExportJobCleanupScheduler.class);

    private final CleanupEventExportJobsUseCase cleanupEventExportJobsUseCase;
    private final EventExportJobCleanupRunRepository cleanupRunRepository;

    public EventExportJobCleanupScheduler(
        CleanupEventExportJobsUseCase cleanupEventExportJobsUseCase,
        EventExportJobCleanupRunRepository cleanupRunRepository
    ) {
        this.cleanupEventExportJobsUseCase = cleanupEventExportJobsUseCase;
        this.cleanupRunRepository = cleanupRunRepository;
    }

    @Scheduled(
        cron = "${events.export.jobs.cleanup-cron:0 15 3 * * *}",
        zone = "${events.export.jobs.cleanup-zone:UTC}"
    )
    public void cleanupFinishedJobs() {
        Instant executedAt = Instant.now();
        CleanupEventExportJobsUseCase.Result result = cleanupEventExportJobsUseCase.cleanup(executedAt);
        cleanupRunRepository.save(new EventExportJobCleanupRun(
            UUID.randomUUID(),
            executedAt,
            result.retentionDays(),
            result.deletedCount()
        ));
        logger.info(
            "event-export cleanup completed: deleted={}, retentionDays={}, completedBeforeExclusive={}",
            result.deletedCount(),
            result.retentionDays(),
            result.completedBeforeExclusive()
        );
    }
}
