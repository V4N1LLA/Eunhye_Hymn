package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.time.Instant;
import java.util.UUID;

public class AdminCreateHymnUseCase {
    private final HymnRepository hymnRepository;

    public AdminCreateHymnUseCase(HymnRepository hymnRepository) {
        this.hymnRepository = hymnRepository;
    }

    public Hymn create(String title, String number, String tags, Boolean enabled) {
        boolean resolvedEnabled = enabled == null || enabled;
        Hymn hymn = new Hymn(UUID.randomUUID(), title, number, tags, resolvedEnabled, Instant.now());
        return hymnRepository.save(hymn);
    }
}
