package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.Event;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository {
    Event save(Event event);

    Optional<Event> findById(UUID id);
}
