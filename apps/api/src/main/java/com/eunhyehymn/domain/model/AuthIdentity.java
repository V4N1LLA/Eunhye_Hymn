package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuthIdentity(
    UUID id,
    UUID userId,
    String provider,
    String providerSubject,
    String email,
    Instant createdAt
) {
}
