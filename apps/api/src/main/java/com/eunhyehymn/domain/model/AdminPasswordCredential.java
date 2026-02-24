package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AdminPasswordCredential(
    UUID id,
    String loginId,
    String passwordHash,
    Instant createdAt,
    Instant updatedAt
) {
}
