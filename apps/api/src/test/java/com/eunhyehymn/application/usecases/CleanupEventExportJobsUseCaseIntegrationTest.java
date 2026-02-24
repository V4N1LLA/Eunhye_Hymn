package com.eunhyehymn.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunhyehymn.domain.model.EventExportJobStatus;
import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.EventExportJobEntity;
import com.eunhyehymn.infrastructure.persistence.EventExportJobJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserEntity;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CleanupEventExportJobsUseCaseIntegrationTest {
    @Autowired private CleanupEventExportJobsUseCase cleanupEventExportJobsUseCase;
    @Autowired private EventExportJobJpaRepository eventExportJobJpaRepository;
    @Autowired private UserJpaRepository userJpaRepository;

    private UUID requestedBy;

    @BeforeEach
    void setUp() {
        eventExportJobJpaRepository.deleteAll();
        requestedBy = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            requestedBy,
            "Cleanup Tester",
            Role.ADMIN,
            UserStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        ));
    }

    @AfterEach
    void tearDown() {
        eventExportJobJpaRepository.deleteAll();
        userJpaRepository.deleteById(requestedBy);
    }

    @Test
    void cleanupDeletesOnlyCompletedOrFailedJobsOutsideRetentionWindow() {
        Instant now = Instant.now();

        UUID oldCompletedId = saveJob(EventExportJobStatus.COMPLETED, now.minus(10, ChronoUnit.DAYS));
        UUID oldFailedId = saveJob(EventExportJobStatus.FAILED, now.minus(8, ChronoUnit.DAYS));
        UUID recentCompletedId = saveJob(EventExportJobStatus.COMPLETED, now.minus(2, ChronoUnit.DAYS));
        UUID runningOldId = saveJob(EventExportJobStatus.RUNNING, null);

        CleanupEventExportJobsUseCase.Result result = cleanupEventExportJobsUseCase.cleanup(now);

        assertThat(result.deletedCount()).isEqualTo(2);
        assertThat(eventExportJobJpaRepository.findById(oldCompletedId)).isEmpty();
        assertThat(eventExportJobJpaRepository.findById(oldFailedId)).isEmpty();
        assertThat(eventExportJobJpaRepository.findById(recentCompletedId)).isPresent();
        assertThat(eventExportJobJpaRepository.findById(runningOldId)).isPresent();
    }

    private UUID saveJob(EventExportJobStatus status, Instant completedAt) {
        UUID id = UUID.randomUUID();
        eventExportJobJpaRepository.save(new EventExportJobEntity(
            id,
            requestedBy,
            EventType.HYMN_OPENED,
            null,
            null,
            null,
            null,
            10_000,
            status,
            status == EventExportJobStatus.COMPLETED ? 5L : null,
            status == EventExportJobStatus.COMPLETED ? "sample.csv" : null,
            status == EventExportJobStatus.COMPLETED ? "id,userId\n" : null,
            status == EventExportJobStatus.FAILED ? "sample error" : null,
            completedAt == null ? Instant.now() : completedAt.minus(1, ChronoUnit.HOURS),
            completedAt == null ? null : completedAt.minus(30, ChronoUnit.MINUTES),
            completedAt
        ));
        return id;
    }
}
