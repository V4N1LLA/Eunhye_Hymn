package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.EventExportJob;
import java.util.Optional;
import java.util.UUID;

public interface EventExportJobRepository {
    EventExportJob save(EventExportJob job);

    Optional<EventExportJob> findById(UUID id);
}
