package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String displayName,
    Role role,
    UserStatus status,
    Instant createdAt,
    Instant lastLoginAt
) {
}
