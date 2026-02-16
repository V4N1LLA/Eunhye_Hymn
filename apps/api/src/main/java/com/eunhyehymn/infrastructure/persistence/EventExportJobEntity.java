package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventExportJobStatus;
import com.eunhyehymn.domain.model.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_export_jobs")
public class EventExportJobEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "hymn_id")
    private UUID hymnId;

    @Column(name = "from_inclusive")
    private Instant fromInclusive;

    @Column(name = "to_exclusive")
    private Instant toExclusive;

    @Column(name = "export_limit", nullable = false)
    private int exportLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventExportJobStatus status;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "csv_content", columnDefinition = "text")
    private String csvContent;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected EventExportJobEntity() {
    }

    public EventExportJobEntity(
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
        this.id = id;
        this.requestedBy = requestedBy;
        this.eventType = eventType;
        this.userId = userId;
        this.hymnId = hymnId;
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.exportLimit = exportLimit;
        this.status = status;
        this.rowCount = rowCount;
        this.fileName = fileName;
        this.csvContent = csvContent;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public EventType getEventType() {
        return eventType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getHymnId() {
        return hymnId;
    }

    public Instant getFromInclusive() {
        return fromInclusive;
    }

    public Instant getToExclusive() {
        return toExclusive;
    }

    public int getExportLimit() {
        return exportLimit;
    }

    public EventExportJobStatus getStatus() {
        return status;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCsvContent() {
        return csvContent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
