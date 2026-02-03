package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.Hymn;
import java.util.Optional;
import java.util.UUID;

public interface HymnRepository {
    Hymn save(Hymn hymn);

    Optional<Hymn> findById(UUID id);
}
