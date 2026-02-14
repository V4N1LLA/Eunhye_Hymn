package com.eunhyehymn.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.domain.model.EventExportJobCleanupRun;
import com.eunhyehymn.domain.model.EventExportJobStatus;
import com.eunhyehymn.domain.repository.EventExportJobCleanupRunRepository;
import com.eunhyehymn.domain.repository.EventExportJobRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetEventExportOpsMetricsUseCaseTest {
    @Test
    void executeCalculatesFailureRateProcessingAndCleanupSummary() {
        Instant now = Instant.parse("2026-02-14T12:00:00Z");
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        InMemoryCleanupRunRepository cleanupRunRepository = new InMemoryCleanupRunRepository();

        jobRepository.metricsRows = List.of(
            new EventExportJobRepository.MetricsRow(
                EventExportJobStatus.COMPLETED,
                now.minus(20, ChronoUnit.MINUTES),
                now.minus(20, ChronoUnit.MINUTES).plusSeconds(10)
            ),
            new EventExportJobRepository.MetricsRow(
                EventExportJobStatus.FAILED,
                now.minus(10, ChronoUnit.MINUTES),
                now.minus(10, ChronoUnit.MINUTES).plusSeconds(30)
            ),
            new EventExportJobRepository.MetricsRow(EventExportJobStatus.RUNNING, now.minus(5, ChronoUnit.MINUTES), null),
            new EventExportJobRepository.MetricsRow(EventExportJobStatus.QUEUED, null, null)
        );
        cleanupRunRepository.summary = new EventExportJobCleanupRunRepository.Summary(2, 9);

        GetEventExportOpsMetricsUseCase useCase = new GetEventExportOpsMetricsUseCase(jobRepository, cleanupRunRepository);

        GetEventExportOpsMetricsUseCase.Result result = useCase.execute(7, now);

        assertThat(jobRepository.lastFromInclusive).isEqualTo(now.minus(7, ChronoUnit.DAYS));
        assertThat(jobRepository.lastToExclusive).isEqualTo(now);
        assertThat(result.jobs().total()).isEqualTo(4);
        assertThat(result.jobs().completed()).isEqualTo(1);
        assertThat(result.jobs().failed()).isEqualTo(1);
        assertThat(result.jobs().failureRatePercent()).isEqualTo(50.0);
        assertThat(result.processing().measuredJobs()).isEqualTo(2);
        assertThat(result.processing().averageSeconds()).isEqualTo(20.0);
        assertThat(result.processing().p95Seconds()).isEqualTo(30.0);
        assertThat(result.cleanup().runCount()).isEqualTo(2);
        assertThat(result.cleanup().deletedJobs()).isEqualTo(9);
    }

    @Test
    void executeNormalizesWindowAndHandlesEmptyMetricsRows() {
        Instant now = Instant.parse("2026-02-14T12:00:00Z");
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        InMemoryCleanupRunRepository cleanupRunRepository = new InMemoryCleanupRunRepository();
        cleanupRunRepository.summary = new EventExportJobCleanupRunRepository.Summary(0, 0);

        GetEventExportOpsMetricsUseCase useCase = new GetEventExportOpsMetricsUseCase(jobRepository, cleanupRunRepository);

        GetEventExportOpsMetricsUseCase.Result result = useCase.execute(0, now);

        assertThat(result.windowDays()).isEqualTo(1);
        assertThat(result.jobs().total()).isZero();
        assertThat(result.jobs().failureRatePercent()).isEqualTo(0.0);
        assertThat(result.processing().measuredJobs()).isZero();
        assertThat(result.processing().averageSeconds()).isEqualTo(0.0);
        assertThat(result.processing().p95Seconds()).isEqualTo(0.0);
    }

    private static class InMemoryEventExportJobRepository implements EventExportJobRepository {
        private List<MetricsRow> metricsRows = new ArrayList<>();
        private Instant lastFromInclusive;
        private Instant lastToExclusive;

        @Override
        public EventExportJob save(EventExportJob job) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EventExportJob> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<MetricsRow> findMetricsRows(Instant fromInclusive, Instant toExclusive) {
            this.lastFromInclusive = fromInclusive;
            this.lastToExclusive = toExclusive;
            return metricsRows;
        }

        @Override
        public long deleteCompletedOrFailedBefore(Instant completedBeforeExclusive) {
            return 0;
        }
    }

    private static class InMemoryCleanupRunRepository implements EventExportJobCleanupRunRepository {
        private Summary summary = new Summary(0, 0);

        @Override
        public EventExportJobCleanupRun save(EventExportJobCleanupRun run) {
            return run;
        }

        @Override
        public Summary summarize(Instant fromInclusive, Instant toExclusive) {
            return summary;
        }
    }
}
