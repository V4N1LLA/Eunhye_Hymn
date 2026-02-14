package com.eunhyehymn.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.domain.repository.EventExportJobRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CleanupEventExportJobsUseCaseTest {
    @Test
    void cleanupUsesConfiguredRetentionDays() {
        InMemoryEventExportJobRepository repository = new InMemoryEventExportJobRepository();
        repository.deletedCount = 3;
        CleanupEventExportJobsUseCase useCase = new CleanupEventExportJobsUseCase(repository, 7);

        Instant now = Instant.parse("2026-02-14T00:00:00Z");
        CleanupEventExportJobsUseCase.Result result = useCase.cleanup(now);

        assertThat(repository.lastCompletedBeforeExclusive).isEqualTo(now.minus(7, ChronoUnit.DAYS));
        assertThat(result.deletedCount()).isEqualTo(3);
        assertThat(result.retentionDays()).isEqualTo(7);
    }

    @Test
    void retentionDaysLowerThanOneIsNormalized() {
        InMemoryEventExportJobRepository repository = new InMemoryEventExportJobRepository();
        CleanupEventExportJobsUseCase useCase = new CleanupEventExportJobsUseCase(repository, 0);

        Instant now = Instant.parse("2026-02-14T00:00:00Z");
        CleanupEventExportJobsUseCase.Result result = useCase.cleanup(now);

        assertThat(result.retentionDays()).isEqualTo(1);
        assertThat(repository.lastCompletedBeforeExclusive).isEqualTo(now.minus(1, ChronoUnit.DAYS));
    }

    private static final class InMemoryEventExportJobRepository implements EventExportJobRepository {
        private Instant lastCompletedBeforeExclusive;
        private long deletedCount;

        @Override
        public EventExportJob save(EventExportJob job) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public Optional<EventExportJob> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<EventExportJob> claimQueued(UUID id, Instant startedAt) {
            return Optional.empty();
        }

        @Override
        public List<UUID> findQueuedJobIds(int limit) {
            return List.of();
        }

        @Override
        public long requeueStaleRunningJobs(Instant staleBeforeExclusive) {
            return 0;
        }

        @Override
        public List<MetricsRow> findMetricsRows(Instant fromInclusive, Instant toExclusive) {
            return List.of();
        }

        @Override
        public long deleteCompletedOrFailedBefore(Instant completedBeforeExclusive) {
            this.lastCompletedBeforeExclusive = completedBeforeExclusive;
            return deletedCount;
        }
    }
}
