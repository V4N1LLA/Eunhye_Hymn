package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventExportJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EventExportJobJpaRepository extends JpaRepository<EventExportJobEntity, UUID> {
    @Query("""
        SELECT e.status AS status, e.startedAt AS startedAt, e.completedAt AS completedAt
        FROM EventExportJobEntity e
        WHERE e.createdAt >= :fromInclusive
          AND e.createdAt < :toExclusive
        """)
    List<MetricsRowProjection> findMetricsRows(
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive
    );

    @Transactional
    long deleteByStatusInAndCompletedAtBefore(Collection<EventExportJobStatus> statuses, Instant completedBeforeExclusive);

    interface MetricsRowProjection {
        EventExportJobStatus getStatus();

        Instant getStartedAt();

        Instant getCompletedAt();
    }
}
