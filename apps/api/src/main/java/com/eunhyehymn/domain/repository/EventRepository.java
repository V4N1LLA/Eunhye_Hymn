package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.Event;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {
    Event save(Event event);

    List<Event> saveAll(List<Event> events);

    Optional<Event> findById(UUID id);
}
