package com.eunhyehymn.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByHymnId(UUID hymnId);
}
