package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ProfileChangeRequest(
    UUID id,
    UUID userId,
    String churchName,
    String name,
    String groupName,
    Gender gender,
    ProfileChangeRequestStatus status,
    Instant requestedAt,
    UUID reviewedBy,
    Instant reviewedAt,
    String rejectReason
) {
}
