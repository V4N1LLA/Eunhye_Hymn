package com.eunhyehymn.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface HymnNoteJpaRepository extends JpaRepository<HymnNoteEntity, UUID> {
    Optional<HymnNoteEntity> findByUserIdAndHymnId(UUID userId, UUID hymnId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByHymnId(UUID hymnId);
}
