package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Hymn(
    UUID id,
    String title,
    String number,
    String tags,
    boolean enabled,
    Instant createdAt
) {
}
