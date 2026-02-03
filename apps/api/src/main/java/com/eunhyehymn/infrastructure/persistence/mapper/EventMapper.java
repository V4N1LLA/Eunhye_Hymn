package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.infrastructure.persistence.EventEntity;

public final class EventMapper {
    private EventMapper() {
    }

    public static Event toDomain(EventEntity entity) {
        return new Event(
            entity.getId(),
            entity.getUserId(),
            entity.getEventType(),
            entity.getHymnId(),
            entity.getPart(),
            entity.getMetadataJson(),
            entity.getCreatedAt()
        );
    }

    public static EventEntity toEntity(Event event) {
        return new EventEntity(
            event.id(),
            event.userId(),
            event.eventType(),
            event.hymnId(),
            event.part(),
            event.metadataJson(),
            event.createdAt()
        );
    }
}
