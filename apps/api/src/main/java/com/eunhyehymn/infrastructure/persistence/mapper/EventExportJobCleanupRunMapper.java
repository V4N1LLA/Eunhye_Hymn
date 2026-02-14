package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.EventExportJobCleanupRun;
import com.eunhyehymn.infrastructure.persistence.EventExportJobCleanupRunEntity;

public final class EventExportJobCleanupRunMapper {
    private EventExportJobCleanupRunMapper() {
    }

    public static EventExportJobCleanupRun toDomain(EventExportJobCleanupRunEntity entity) {
        return new EventExportJobCleanupRun(
            entity.getId(),
            entity.getExecutedAt(),
            entity.getRetentionDays(),
            entity.getDeletedCount()
        );
    }

    public static EventExportJobCleanupRunEntity toEntity(EventExportJobCleanupRun run) {
        return new EventExportJobCleanupRunEntity(
            run.id(),
            run.executedAt(),
            run.retentionDays(),
            run.deletedCount()
        );
    }
}
