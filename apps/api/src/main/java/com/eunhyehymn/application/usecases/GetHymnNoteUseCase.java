package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.HymnNote;
import com.eunhyehymn.domain.repository.HymnNoteRepository;
import java.util.Optional;
import java.util.UUID;

public class GetHymnNoteUseCase {
    private final HymnNoteRepository hymnNoteRepository;

    public GetHymnNoteUseCase(HymnNoteRepository hymnNoteRepository) {
        this.hymnNoteRepository = hymnNoteRepository;
    }

    public Optional<HymnNote> get(UUID userId, UUID hymnId) {
        return hymnNoteRepository.findByUserIdAndHymnId(userId, hymnId);
    }
}
