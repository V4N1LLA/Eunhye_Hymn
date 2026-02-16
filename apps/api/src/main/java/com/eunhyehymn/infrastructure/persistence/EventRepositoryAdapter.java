package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.repository.EventRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.EventMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepositoryAdapter implements EventRepository {
    private final EventJpaRepository eventJpaRepository;

    public EventRepositoryAdapter(EventJpaRepository eventJpaRepository) {
        this.eventJpaRepository = eventJpaRepository;
    }

    @Override
    public Event save(Event event) {
        EventEntity saved = eventJpaRepository.save(EventMapper.toEntity(event));
        return EventMapper.toDomain(saved);
    }

    @Override
    public List<Event> saveAll(List<Event> events) {
        List<EventEntity> saved = eventJpaRepository.saveAll(events.stream().map(EventMapper::toEntity).toList());
        return saved.stream().map(EventMapper::toDomain).toList();
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return eventJpaRepository.findById(id).map(EventMapper::toDomain);
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
        return eventJpaRepository.findRecent(
            fromInclusive,
            toExclusive,
            eventType,
            userId,
            hymnId,
            PageRequest.of(Math.max(page - 1, 0), size)
        ).stream().map(EventMapper::toDomain).toList();
    }

    @Override
    public long countRecent(Instant fromInclusive, Instant toExclusive, EventType eventType, UUID userId, UUID hymnId) {
        return eventJpaRepository.countRecent(fromInclusive, toExclusive, eventType, userId, hymnId);
    }

    @Override
    public List<EventTypeCount> countByEventType(
        Instant fromInclusive,
        Instant toExclusive,
        EventType eventType,
        UUID userId,
        UUID hymnId
    ) {
        return eventJpaRepository.countByEventType(fromInclusive, toExclusive, eventType, userId, hymnId).stream()
            .map(row -> new EventTypeCount(row.getEventType(), row.getTotal()))
            .toList();
    }

    @Override
    public void deleteByHymnId(UUID hymnId) {
        eventJpaRepository.deleteByHymnId(hymnId);
    }
}
