package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.HymnNote;
import com.eunhyehymn.domain.repository.HymnNoteRepository;
import java.time.Instant;
import java.util.UUID;

public class SaveHymnNoteUseCase {
    private final HymnNoteRepository hymnNoteRepository;

    public SaveHymnNoteUseCase(HymnNoteRepository hymnNoteRepository) {
        this.hymnNoteRepository = hymnNoteRepository;
    }

    public HymnNote save(UUID userId, UUID hymnId, String content) {
        Instant now = Instant.now();
        HymnNote note = hymnNoteRepository.findByUserIdAndHymnId(userId, hymnId)
            .map(existing -> new HymnNote(existing.id(), userId, hymnId, content, now))
            .orElseGet(() -> new HymnNote(UUID.randomUUID(), userId, hymnId, content, now));

        return hymnNoteRepository.save(note);
    }
}
