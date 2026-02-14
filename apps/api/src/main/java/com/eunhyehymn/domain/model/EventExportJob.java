package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EventExportJob(
    UUID id,
    UUID requestedBy,
    EventType eventType,
    UUID userId,
    UUID hymnId,
    Instant fromInclusive,
    Instant toExclusive,
    int exportLimit,
    EventExportJobStatus status,
    Long rowCount,
    String fileName,
    String csvContent,
    String errorMessage,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt
) {
    public EventExportJob markRunning(Instant startedAt) {
        return new EventExportJob(
            id,
            requestedBy,
            eventType,
            userId,
            hymnId,
            fromInclusive,
            toExclusive,
            exportLimit,
            EventExportJobStatus.RUNNING,
            null,
            null,
            null,
            null,
            createdAt,
            startedAt,
            null
        );
    }

    public EventExportJob markCompleted(long rowCount, String fileName, String csvContent, Instant completedAt) {
        return new EventExportJob(
            id,
            requestedBy,
            eventType,
            userId,
            hymnId,
            fromInclusive,
            toExclusive,
            exportLimit,
            EventExportJobStatus.COMPLETED,
            rowCount,
            fileName,
            csvContent,
            null,
            createdAt,
            startedAt,
            completedAt
        );
    }

    public EventExportJob markFailed(String errorMessage, Instant completedAt) {
        return new EventExportJob(
            id,
            requestedBy,
            eventType,
            userId,
            hymnId,
            fromInclusive,
            toExclusive,
            exportLimit,
            EventExportJobStatus.FAILED,
            null,
            null,
            null,
            errorMessage,
            createdAt,
            startedAt,
            completedAt
        );
    }
}
