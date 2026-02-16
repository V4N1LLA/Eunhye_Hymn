package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {
    @Query("""
        SELECT e
        FROM EventEntity e
        WHERE (:fromInclusive IS NULL OR e.createdAt >= :fromInclusive)
          AND (:toExclusive IS NULL OR e.createdAt < :toExclusive)
          AND (:eventType IS NULL OR e.eventType = :eventType)
          AND (:userId IS NULL OR e.userId = :userId)
          AND (:hymnId IS NULL OR e.hymnId = :hymnId)
        ORDER BY e.createdAt DESC, e.id DESC
        """)
    List<EventEntity> findRecent(
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive,
        @Param("eventType") EventType eventType,
        @Param("userId") UUID userId,
        @Param("hymnId") UUID hymnId,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(e)
        FROM EventEntity e
        WHERE (:fromInclusive IS NULL OR e.createdAt >= :fromInclusive)
          AND (:toExclusive IS NULL OR e.createdAt < :toExclusive)
          AND (:eventType IS NULL OR e.eventType = :eventType)
          AND (:userId IS NULL OR e.userId = :userId)
          AND (:hymnId IS NULL OR e.hymnId = :hymnId)
        """)
    long countRecent(
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive,
        @Param("eventType") EventType eventType,
        @Param("userId") UUID userId,
        @Param("hymnId") UUID hymnId
    );

    @Query("""
        SELECT e.eventType AS eventType, COUNT(e) AS total
        FROM EventEntity e
        WHERE (:fromInclusive IS NULL OR e.createdAt >= :fromInclusive)
          AND (:toExclusive IS NULL OR e.createdAt < :toExclusive)
          AND (:eventType IS NULL OR e.eventType = :eventType)
          AND (:userId IS NULL OR e.userId = :userId)
          AND (:hymnId IS NULL OR e.hymnId = :hymnId)
        GROUP BY e.eventType
        """)
    List<EventTypeCountProjection> countByEventType(
        @Param("fromInclusive") Instant fromInclusive,
        @Param("toExclusive") Instant toExclusive,
        @Param("eventType") EventType eventType,
        @Param("userId") UUID userId,
        @Param("hymnId") UUID hymnId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByHymnId(UUID hymnId);

    interface EventTypeCountProjection {
        EventType getEventType();

        long getTotal();
    }
}
