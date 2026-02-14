package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventExportJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EventExportJobJpaRepository extends JpaRepository<EventExportJobEntity, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE EventExportJobEntity e
        SET e.status = :runningStatus,
            e.startedAt = :startedAt,
            e.completedAt = NULL,
            e.rowCount = NULL,
            e.fileName = NULL,
            e.csvContent = NULL,
            e.errorMessage = NULL
        WHERE e.id = :id
          AND e.status = :queuedStatus
        """)
    int claimQueued(
        @Param("id") UUID id,
        @Param("startedAt") Instant startedAt,
        @Param("queuedStatus") EventExportJobStatus queuedStatus,
        @Param("runningStatus") EventExportJobStatus runningStatus
    );

    @Query("""
        SELECT e.id
        FROM EventExportJobEntity e
        WHERE e.status = :status
        ORDER BY e.createdAt ASC, e.id ASC
        """)
    List<UUID> findQueuedJobIds(
        @Param("status") EventExportJobStatus status,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE EventExportJobEntity e
        SET e.status = :queuedStatus,
            e.startedAt = NULL,
            e.completedAt = NULL,
            e.rowCount = NULL,
            e.fileName = NULL,
            e.csvContent = NULL,
            e.errorMessage = :requeuedMessage
        WHERE e.status = :runningStatus
          AND e.startedAt IS NOT NULL
          AND e.startedAt < :staleBeforeExclusive
        """)
    int requeueStaleRunningJobs(
        @Param("staleBeforeExclusive") Instant staleBeforeExclusive,
        @Param("runningStatus") EventExportJobStatus runningStatus,
        @Param("queuedStatus") EventExportJobStatus queuedStatus,
        @Param("requeuedMessage") String requeuedMessage
    );

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
