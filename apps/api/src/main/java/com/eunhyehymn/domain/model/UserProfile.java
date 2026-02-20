package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(
    UUID userId,
    String churchName,
    String name,
    String groupName,
    Gender gender,
    Instant updatedAt
) {
}
