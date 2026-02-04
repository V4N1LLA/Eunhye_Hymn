package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Event(
    UUID id,
    UUID userId,
    EventType eventType,
    UUID hymnId,
    PartType part,
    String metadataJson,
    Instant createdAt
) {
}
