package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
    UUID id,
    UUID userId,
    String tokenHash,
    Instant expiresAt,
    Instant revokedAt,
    Instant createdAt
) {
}
