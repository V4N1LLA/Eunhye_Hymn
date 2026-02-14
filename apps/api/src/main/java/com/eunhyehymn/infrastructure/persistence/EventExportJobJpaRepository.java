package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventExportJobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface EventExportJobJpaRepository extends JpaRepository<EventExportJobEntity, UUID> {
    @Transactional
    long deleteByStatusInAndCompletedAtBefore(Collection<EventExportJobStatus> statuses, Instant completedBeforeExclusive);
}
