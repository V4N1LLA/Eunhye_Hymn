package com.eunhyehymn.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_export_job_cleanup_runs")
public class EventExportJobCleanupRunEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "deleted_count", nullable = false)
    private long deletedCount;

    protected EventExportJobCleanupRunEntity() {
    }

    public EventExportJobCleanupRunEntity(
        UUID id,
        Instant executedAt,
        int retentionDays,
        long deletedCount
    ) {
        this.id = id;
        this.executedAt = executedAt;
        this.retentionDays = retentionDays;
        this.deletedCount = deletedCount;
    }

    public UUID getId() {
        return id;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public long getDeletedCount() {
        return deletedCount;
    }
}
