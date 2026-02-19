package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserPasswordCredential(
    UUID userId,
    String passwordHash,
    Instant createdAt,
    Instant updatedAt
) {
}
