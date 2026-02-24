package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventExportJobCleanupRun;
import com.eunhyehymn.domain.repository.EventExportJobCleanupRunRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.EventExportJobCleanupRunMapper;
import java.time.Instant;
import org.springframework.stereotype.Repository;

@Repository
public class EventExportJobCleanupRunRepositoryAdapter implements EventExportJobCleanupRunRepository {
    private final EventExportJobCleanupRunJpaRepository jpaRepository;

    public EventExportJobCleanupRunRepositoryAdapter(EventExportJobCleanupRunJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EventExportJobCleanupRun save(EventExportJobCleanupRun run) {
        EventExportJobCleanupRunEntity saved = jpaRepository.save(EventExportJobCleanupRunMapper.toEntity(run));
        return EventExportJobCleanupRunMapper.toDomain(saved);
    }

    @Override
    public Summary summarize(Instant fromInclusive, Instant toExclusive) {
        EventExportJobCleanupRunJpaRepository.SummaryProjection projection = jpaRepository.summarize(fromInclusive, toExclusive);
        if (projection == null) {
            return new Summary(0, 0);
        }
        return new Summary(projection.getRunCount(), projection.getDeletedCount());
    }
}
