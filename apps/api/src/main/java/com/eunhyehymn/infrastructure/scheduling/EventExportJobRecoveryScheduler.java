package com.eunhyehymn.infrastructure.scheduling;

import com.eunhyehymn.application.usecases.AdminEventExportJobUseCase;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventExportJobRecoveryScheduler {
    private static final Logger logger = LoggerFactory.getLogger(EventExportJobRecoveryScheduler.class);

    private final AdminEventExportJobUseCase adminEventExportJobUseCase;
    private final TaskExecutor eventExportTaskExecutor;
    private final int staleRunningMinutes;
    private final int dispatchBatchSize;

    public EventExportJobRecoveryScheduler(
        AdminEventExportJobUseCase adminEventExportJobUseCase,
        @Qualifier("eventExportTaskExecutor") TaskExecutor eventExportTaskExecutor,
        @Value("${events.export.jobs.recovery.stale-running-minutes:15}") int staleRunningMinutes,
        @Value("${events.export.jobs.recovery.dispatch-batch-size:20}") int dispatchBatchSize
    ) {
        this.adminEventExportJobUseCase = adminEventExportJobUseCase;
        this.eventExportTaskExecutor = eventExportTaskExecutor;
        this.staleRunningMinutes = normalizePositive(staleRunningMinutes, 15);
        this.dispatchBatchSize = normalizePositive(dispatchBatchSize, 20);
    }

    @Scheduled(
        cron = "${events.export.jobs.recovery.cron:0/30 * * * * *}",
        zone = "${events.export.jobs.recovery.zone:UTC}"
    )
    public void recoverAndDispatch() {
        Instant now = Instant.now();
        Instant staleBeforeExclusive = now.minus(staleRunningMinutes, ChronoUnit.MINUTES);
        long requeued = adminEventExportJobUseCase.requeueStaleRunningJobs(staleBeforeExclusive);

        List<UUID> queuedJobIds = adminEventExportJobUseCase.findQueuedJobIds(dispatchBatchSize);
        int dispatched = 0;
        for (UUID jobId : queuedJobIds) {
            try {
                eventExportTaskExecutor.execute(() -> adminEventExportJobUseCase.process(jobId));
                dispatched += 1;
            } catch (TaskRejectedException ex) {
                logger.warn("event export recovery dispatch rejected for jobId={}", jobId, ex);
                break;
            }
        }

        if (requeued > 0 || dispatched > 0) {
            logger.info(
                "event-export recovery cycle: requeued={}, dispatched={}, staleRunningMinutes={}, dispatchBatchSize={}",
                requeued,
                dispatched,
                staleRunningMinutes,
                dispatchBatchSize
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
