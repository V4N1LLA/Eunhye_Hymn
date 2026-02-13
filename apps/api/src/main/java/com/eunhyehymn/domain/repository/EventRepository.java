package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {
    Event save(Event event);

    List<Event> saveAll(List<Event> events);

    Optional<Event> findById(UUID id);

    List<Event> findRecent(
        Instant fromInclusive,
        Instant toExclusive,
        EventType eventType,
        UUID userId,
        UUID hymnId,
        int page,
        int size
    );

    long countRecent(Instant fromInclusive, Instant toExclusive, EventType eventType, UUID userId, UUID hymnId);

    List<EventTypeCount> countByEventType(
        Instant fromInclusive,
        Instant toExclusive,
        EventType eventType,
        UUID userId,
        UUID hymnId
    );

    void deleteByHymnId(UUID hymnId);

    record EventTypeCount(EventType eventType, long count) {
    }
}
