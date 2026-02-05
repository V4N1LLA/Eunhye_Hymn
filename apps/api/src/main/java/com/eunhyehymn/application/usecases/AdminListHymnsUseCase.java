package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.util.List;

public class AdminListHymnsUseCase {
    private final HymnRepository hymnRepository;

    public AdminListHymnsUseCase(HymnRepository hymnRepository) {
        this.hymnRepository = hymnRepository;
    }

    public List<Hymn> listAll() {
        return hymnRepository.findAll();
    }
}
