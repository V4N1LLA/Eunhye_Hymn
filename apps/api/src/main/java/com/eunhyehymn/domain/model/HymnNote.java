package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record HymnNote(
    UUID id,
    UUID userId,
    UUID hymnId,
    String content,
    Instant updatedAt
) {
}
