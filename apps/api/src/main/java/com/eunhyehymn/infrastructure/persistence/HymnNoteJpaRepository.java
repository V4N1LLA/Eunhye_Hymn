package com.eunhyehymn.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HymnNoteJpaRepository extends JpaRepository<HymnNoteEntity, UUID> {
    Optional<HymnNoteEntity> findByUserIdAndHymnId(UUID userId, UUID hymnId);
}
