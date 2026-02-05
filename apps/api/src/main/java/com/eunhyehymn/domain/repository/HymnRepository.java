package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.Hymn;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HymnRepository {
    Hymn save(Hymn hymn);

    List<Hymn> findEnabled();

    List<Hymn> findAll();

    Optional<Hymn> findById(UUID id);

    List<Hymn> findByIdIn(List<UUID> ids);
}
