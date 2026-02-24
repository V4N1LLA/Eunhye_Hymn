package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SmsVerificationRequest(
    UUID id,
    UUID userId,
    String phoneNumber,
    String codeHash,
    Instant expiresAt,
    int attempts,
    Instant verifiedAt,
    Instant createdAt,
    Instant updatedAt
) {
    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
