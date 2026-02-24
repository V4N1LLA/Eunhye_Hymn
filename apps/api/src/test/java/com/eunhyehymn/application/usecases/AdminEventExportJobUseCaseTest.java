package com.eunhyehymn.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.domain.model.EventExportJobStatus;
import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.repository.EventExportJobRepository;
import com.eunhyehymn.domain.repository.EventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AdminEventExportJobUseCaseTest {
    @Test
    void createSnapshotsToExclusiveWhenUpperBoundIsOmitted() {
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        AdminEventExportJobUseCase useCase = new AdminEventExportJobUseCase(new NoopEventRepository(), jobRepository);
        Instant before = Instant.now();

        EventExportJob created = useCase.create(new AdminEventExportJobUseCase.CreateCommand(
            UUID.randomUUID(),
            EventType.HYMN_OPENED,
            null,
            null,
            null,
            null,
            null
        ));

        Instant after = Instant.now();
        assertThat(created.toExclusive()).isNotNull();
        assertThat(created.toExclusive()).isBetween(before, after);
    }

    @Test
    void processReturnsCurrentJobWhenClaimFails() {
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        AdminEventExportJobUseCase useCase = new AdminEventExportJobUseCase(new NoopEventRepository(), jobRepository);
        EventExportJob queuedJob = new EventExportJob(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            Instant.now(),
            100,
            EventExportJobStatus.QUEUED,
            null,
            null,
            null,
            null,
            Instant.now(),
            null,
            null
        );
        jobRepository.findByIdResult = Optional.of(queuedJob);
        jobRepository.claimQueuedResult = Optional.empty();

        EventExportJob result = useCase.process(queuedJob.id());

        assertThat(result.status()).isEqualTo(EventExportJobStatus.QUEUED);
        assertThat(jobRepository.savedJobs).isEmpty();
    }

    @Test
    void findQueuedJobIdsNormalizesBatchSize() {
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        AdminEventExportJobUseCase useCase = new AdminEventExportJobUseCase(new NoopEventRepository(), jobRepository);
        UUID queuedId = UUID.randomUUID();
        jobRepository.queuedIds = List.of(queuedId);

        List<UUID> idsForZero = useCase.findQueuedJobIds(0);
        List<UUID> idsForLarge = useCase.findQueuedJobIds(999);

        assertThat(idsForZero).containsExactly(queuedId);
        assertThat(idsForLarge).containsExactly(queuedId);
        assertThat(jobRepository.findQueuedLimitHistory).containsExactly(1, 200);
    }

    @Test
    void processMarksJobFailedAndTruncatesLongErrorMessage() {
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        NoopEventRepository eventRepository = new NoopEventRepository();
        AdminEventExportJobUseCase useCase = new AdminEventExportJobUseCase(eventRepository, jobRepository);
        EventExportJob runningJob = new EventExportJob(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            Instant.now(),
            100,
            EventExportJobStatus.RUNNING,
            null,
            null,
            null,
            null,
            Instant.now(),
            Instant.now(),
            null
        );
        String longMessage = "x".repeat(700);
        eventRepository.findRecentException = new RuntimeException(longMessage);
        jobRepository.claimQueuedResult = Optional.of(runningJob);

        EventExportJob result = useCase.process(runningJob.id());

        assertThat(result.status()).isEqualTo(EventExportJobStatus.FAILED);
        assertThat(result.errorMessage()).hasSize(500);
        assertThat(jobRepository.savedJobs).hasSize(1);
    }

    @Test
    void getDownloadThrowsConflictWhenJobFailed() {
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        AdminEventExportJobUseCase useCase = new AdminEventExportJobUseCase(new NoopEventRepository(), jobRepository);
        UUID requestedBy = UUID.randomUUID();
        EventExportJob failedJob = new EventExportJob(
            UUID.randomUUID(),
            requestedBy,
            null,
            null,
            null,
            null,
            Instant.now(),
            100,
            EventExportJobStatus.FAILED,
            null,
            null,
            null,
            "Export failed for timeout",
            Instant.now(),
            Instant.now(),
            Instant.now()
        );
        jobRepository.findByIdResult = Optional.of(failedJob);

        assertThatThrownBy(() -> useCase.getDownload(failedJob.id(), requestedBy))
            .isInstanceOf(ApiException.class)
            .satisfies(throwable -> {
                ApiException ex = (ApiException) throwable;
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(ex.getCode()).isEqualTo("export_job_failed");
                assertThat(ex).hasMessage("Export failed for timeout");
            });
    }

    @Test
    void getDownloadThrowsServerErrorWhenCompletedResultIsMissing() {
        InMemoryEventExportJobRepository jobRepository = new InMemoryEventExportJobRepository();
        AdminEventExportJobUseCase useCase = new AdminEventExportJobUseCase(new NoopEventRepository(), jobRepository);
        UUID requestedBy = UUID.randomUUID();
        EventExportJob corruptedCompletedJob = new EventExportJob(
            UUID.randomUUID(),
            requestedBy,
            null,
            null,
            null,
            null,
            Instant.now(),
            100,
            EventExportJobStatus.COMPLETED,
            10L,
            null,
            "csv-content",
            null,
            Instant.now(),
            Instant.now(),
            Instant.now()
        );
        jobRepository.findByIdResult = Optional.of(corruptedCompletedJob);

        assertThatThrownBy(() -> useCase.getDownload(corruptedCompletedJob.id(), requestedBy))
            .isInstanceOf(ApiException.class)
            .satisfies(throwable -> {
                ApiException ex = (ApiException) throwable;
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(ex.getCode()).isEqualTo("export_job_corrupted");
            });
    }

    private static final class InMemoryEventExportJobRepository implements EventExportJobRepository {
        private Optional<EventExportJob> findByIdResult = Optional.empty();
        private Optional<EventExportJob> claimQueuedResult = Optional.empty();
        private List<UUID> queuedIds = List.of();
        private final List<EventExportJob> savedJobs = new ArrayList<>();
        private final List<Integer> findQueuedLimitHistory = new ArrayList<>();

        @Override
        public EventExportJob save(EventExportJob job) {
            savedJobs.add(job);
            findByIdResult = Optional.of(job);
            return job;
        }

        @Override
        public Optional<EventExportJob> findById(UUID id) {
            return findByIdResult.filter(job -> job.id().equals(id));
        }

        @Override
        public Optional<EventExportJob> claimQueued(UUID id, Instant startedAt) {
            return claimQueuedResult;
        }

        @Override
        public List<UUID> findQueuedJobIds(int limit) {
            findQueuedLimitHistory.add(limit);
            return queuedIds;
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
            return 0;
        }
    }

    private static final class NoopEventRepository implements EventRepository {
        private RuntimeException findRecentException;

        @Override
        public Event save(Event event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Event> saveAll(List<Event> events) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Event> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<Event> findRecent(
            Instant fromInclusive,
            Instant toExclusive,
            EventType eventType,
            UUID userId,
            UUID hymnId,
            int page,
            int size
        ) {
            if (findRecentException != null) {
                throw findRecentException;
            }
            return List.of();
        }

        @Override
        public long countRecent(Instant fromInclusive, Instant toExclusive, EventType eventType, UUID userId, UUID hymnId) {
            return 0;
        }

        @Override
        public List<EventTypeCount> countByEventType(
            Instant fromInclusive,
            Instant toExclusive,
            EventType eventType,
            UUID userId,
            UUID hymnId
        ) {
            return List.of();
        }

        @Override
        public void deleteByHymnId(UUID hymnId) {
        }
    }
}
