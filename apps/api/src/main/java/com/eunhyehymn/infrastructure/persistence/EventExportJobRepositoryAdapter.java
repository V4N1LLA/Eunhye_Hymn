package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.domain.repository.EventExportJobRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.EventExportJobMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class EventExportJobRepositoryAdapter implements EventExportJobRepository {
    private final EventExportJobJpaRepository jpaRepository;

    public EventExportJobRepositoryAdapter(EventExportJobJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EventExportJob save(EventExportJob job) {
        EventExportJobEntity saved = jpaRepository.save(EventExportJobMapper.toEntity(job));
        return EventExportJobMapper.toDomain(saved);
    }

    @Override
    public Optional<EventExportJob> findById(UUID id) {
        return jpaRepository.findById(id).map(EventExportJobMapper::toDomain);
    }
}
