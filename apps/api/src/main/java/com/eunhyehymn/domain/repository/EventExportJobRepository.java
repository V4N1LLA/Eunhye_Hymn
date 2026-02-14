package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.domain.model.EventExportJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventExportJobRepository {
    EventExportJob save(EventExportJob job);

    Optional<EventExportJob> findById(UUID id);

    List<MetricsRow> findMetricsRows(Instant fromInclusive, Instant toExclusive);

    long deleteCompletedOrFailedBefore(Instant completedBeforeExclusive);

    record MetricsRow(
        EventExportJobStatus status,
        Instant startedAt,
        Instant completedAt
    ) {
    }
}
