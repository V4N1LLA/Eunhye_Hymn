package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.infrastructure.persistence.EventExportJobEntity;

public final class EventExportJobMapper {
    private EventExportJobMapper() {
    }

    public static EventExportJob toDomain(EventExportJobEntity entity) {
        return new EventExportJob(
            entity.getId(),
            entity.getRequestedBy(),
            entity.getEventType(),
            entity.getUserId(),
            entity.getHymnId(),
            entity.getFromInclusive(),
            entity.getToExclusive(),
            entity.getExportLimit(),
            entity.getStatus(),
            entity.getRowCount(),
            entity.getFileName(),
            entity.getCsvContent(),
            entity.getErrorMessage(),
            entity.getCreatedAt(),
            entity.getStartedAt(),
            entity.getCompletedAt()
        );
    }

    public static EventExportJobEntity toEntity(EventExportJob job) {
        return new EventExportJobEntity(
            job.id(),
            job.requestedBy(),
            job.eventType(),
            job.userId(),
            job.hymnId(),
            job.fromInclusive(),
            job.toExclusive(),
            job.exportLimit(),
            job.status(),
            job.rowCount(),
            job.fileName(),
            job.csvContent(),
            job.errorMessage(),
            job.createdAt(),
            job.startedAt(),
            job.completedAt()
        );
    }
}
