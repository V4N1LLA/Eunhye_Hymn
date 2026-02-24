package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.repository.EventRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.EventMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepositoryAdapter implements EventRepository {
    private final EventJpaRepository eventJpaRepository;
    private final EntityManager entityManager;

    public EventRepositoryAdapter(EventJpaRepository eventJpaRepository, EntityManager entityManager) {
        this.eventJpaRepository = eventJpaRepository;
        this.entityManager = entityManager;
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
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.max(size, 1);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EventEntity> query = cb.createQuery(EventEntity.class);
        Root<EventEntity> root = query.from(EventEntity.class);

        query.select(root);
        query.where(buildPredicates(fromInclusive, toExclusive, eventType, userId, hymnId, cb, root));
        query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));

        List<EventEntity> items = entityManager.createQuery(query)
            .setFirstResult((normalizedPage - 1) * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();
        return items.stream().map(EventMapper::toDomain).toList();
    }

    @Override
    public long countRecent(Instant fromInclusive, Instant toExclusive, EventType eventType, UUID userId, UUID hymnId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<EventEntity> root = query.from(EventEntity.class);

        query.select(cb.count(root));
        query.where(buildPredicates(fromInclusive, toExclusive, eventType, userId, hymnId, cb, root));
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public List<EventTypeCount> countByEventType(
        Instant fromInclusive,
        Instant toExclusive,
        EventType eventType,
        UUID userId,
        UUID hymnId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<EventEntity> root = query.from(EventEntity.class);
        Path<EventType> eventTypePath = root.get("eventType");

        query.multiselect(
            eventTypePath.alias("eventType"),
            cb.count(root).alias("total")
        );
        query.where(buildPredicates(fromInclusive, toExclusive, eventType, userId, hymnId, cb, root));
        query.groupBy(eventTypePath);

        return entityManager.createQuery(query).getResultList().stream()
            .map(row -> new EventTypeCount(
                row.get("eventType", EventType.class),
                row.get("total", Number.class).longValue()
            ))
            .toList();
    }

    @Override
    public void deleteByHymnId(UUID hymnId) {
        eventJpaRepository.deleteByHymnId(hymnId);
    }

    private Predicate[] buildPredicates(
        Instant fromInclusive,
        Instant toExclusive,
        EventType eventType,
        UUID userId,
        UUID hymnId,
        CriteriaBuilder cb,
        Root<EventEntity> root
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (fromInclusive != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive));
        }
        if (toExclusive != null) {
            predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
        }
        if (eventType != null) {
            predicates.add(cb.equal(root.get("eventType"), eventType));
        }
        if (userId != null) {
            predicates.add(cb.equal(root.get("userId"), userId));
        }
        if (hymnId != null) {
            predicates.add(cb.equal(root.get("hymnId"), hymnId));
        }
        return predicates.toArray(Predicate[]::new);
    }
}
