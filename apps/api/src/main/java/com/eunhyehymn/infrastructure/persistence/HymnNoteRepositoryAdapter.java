package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.HymnNote;
import com.eunhyehymn.domain.repository.HymnNoteRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.HymnNoteMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class HymnNoteRepositoryAdapter implements HymnNoteRepository {
    private final HymnNoteJpaRepository hymnNoteJpaRepository;

    public HymnNoteRepositoryAdapter(HymnNoteJpaRepository hymnNoteJpaRepository) {
        this.hymnNoteJpaRepository = hymnNoteJpaRepository;
    }

    @Override
    public HymnNote save(HymnNote note) {
        HymnNoteEntity saved = hymnNoteJpaRepository.save(HymnNoteMapper.toEntity(note));
        return HymnNoteMapper.toDomain(saved);
    }

    @Override
    public Optional<HymnNote> findById(UUID id) {
        return hymnNoteJpaRepository.findById(id).map(HymnNoteMapper::toDomain);
    }

    @Override
    public Optional<HymnNote> findByUserIdAndHymnId(UUID userId, UUID hymnId) {
        return hymnNoteJpaRepository.findByUserIdAndHymnId(userId, hymnId).map(HymnNoteMapper::toDomain);
    }
}
