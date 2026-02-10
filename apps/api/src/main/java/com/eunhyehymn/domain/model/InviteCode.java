package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record InviteCode(
    UUID id,
    String code,
    Integer maxUses,
    int usedCount,
    Instant expiresAt,
    Instant revokedAt,
    Instant createdAt
) {
    public boolean isValid() {
        if (revokedAt != null) return false;
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) return false;
        if (maxUses != null && usedCount >= maxUses) return false;
        return true;
    }
}
