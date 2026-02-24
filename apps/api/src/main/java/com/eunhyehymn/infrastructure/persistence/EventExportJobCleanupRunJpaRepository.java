package com.eunhyehymn.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventExportJobCleanupRunJpaRepository extends JpaRepository<EventExportJobCleanupRunEntity, UUID> {
    @Query("""
        SELECT COUNT(r) AS runCount, COALESCE(SUM(r.deletedCount), 0) AS deletedCount
        FROM EventExportJobCleanupRunEntity r
        WHERE r.executedAt >= :fromInclusive
          AND r.executedAt < :toExclusive
        """)
    SummaryProjection summarize(
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive
    );

    interface SummaryProjection {
        long getRunCount();

        long getDeletedCount();
    }
}
