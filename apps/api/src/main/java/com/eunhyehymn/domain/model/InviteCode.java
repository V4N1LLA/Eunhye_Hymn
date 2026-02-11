package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record InviteCode(
    String code,
    UUID createdBy,
    String description,
    Integer maxUses,
    int usedCount,
    boolean enabled,
    Instant expiresAt,
    Instant createdAt
) {
    public InviteCode withEnabled(boolean enabled) {
        return new InviteCode(code, createdBy, description, maxUses, usedCount, enabled, expiresAt, createdAt);
    }
}
