package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.util.List;

public class ListHymnsUseCase {
    private final HymnRepository hymnRepository;

    public ListHymnsUseCase(HymnRepository hymnRepository) {
        this.hymnRepository = hymnRepository;
    }

    public List<Hymn> listEnabled() {
        return hymnRepository.findEnabled();
    }
}
