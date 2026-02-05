package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.HymnNote;
import java.util.Optional;
import java.util.UUID;

public interface HymnNoteRepository {
    HymnNote save(HymnNote note);

    Optional<HymnNote> findById(UUID id);

    Optional<HymnNote> findByUserIdAndHymnId(UUID userId, UUID hymnId);
}
