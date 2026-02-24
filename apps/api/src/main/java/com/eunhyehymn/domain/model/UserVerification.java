package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserVerification(
    UUID userId,
    String inviteCode,
    Instant inviteVerifiedAt,
    String phoneNumber,
    Instant phoneVerifiedAt,
    Instant updatedAt
) {
    public boolean isInviteVerified() {
        return inviteVerifiedAt != null;
    }

    public boolean isPhoneVerified() {
        return phoneVerifiedAt != null;
    }

    public boolean isCompleted() {
        return isInviteVerified() && isPhoneVerified();
    }
}
