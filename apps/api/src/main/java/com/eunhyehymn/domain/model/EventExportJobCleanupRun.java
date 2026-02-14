package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EventExportJobCleanupRun(
    UUID id,
    Instant executedAt,
    int retentionDays,
    long deletedCount
) {
}
