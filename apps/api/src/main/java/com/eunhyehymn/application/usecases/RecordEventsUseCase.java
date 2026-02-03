package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.model.PartType;
import com.eunhyehymn.domain.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class RecordEventsUseCase {
    private final EventRepository eventRepository;

    public RecordEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void record(UUID userId, List<EventInput> inputs) {
        List<Event> events = inputs.stream().map(input -> {
            EventType eventType = parseEventType(input.eventType());
            PartType part = parsePart(input.part());
            return new Event(
                UUID.randomUUID(),
                userId,
                eventType,
                input.hymnId(),
                part,
                input.metadataJson(),
                Instant.now()
            );
        }).toList();

        eventRepository.saveAll(events);
    }

    private EventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_event_type", "이벤트 타입이 유효하지 않습니다", null);
        }
        try {
            return EventType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_event_type", "이벤트 타입이 유효하지 않습니다", null);
        }
    }

    private PartType parsePart(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PartType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_part", "파트 값이 유효하지 않습니다", null);
        }
    }

    public record EventInput(
        String eventType,
        UUID hymnId,
        String part,
        String metadataJson
    ) {
    }
}
