package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.HymnMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class HymnRepositoryAdapter implements HymnRepository {
    private final HymnJpaRepository hymnJpaRepository;

    public HymnRepositoryAdapter(HymnJpaRepository hymnJpaRepository) {
        this.hymnJpaRepository = hymnJpaRepository;
    }

    @Override
    public Hymn save(Hymn hymn) {
        HymnEntity saved = hymnJpaRepository.save(HymnMapper.toEntity(hymn));
        return HymnMapper.toDomain(saved);
    }

    @Override
    public List<Hymn> findEnabled() {
        return hymnJpaRepository.findByEnabledTrue().stream().map(HymnMapper::toDomain).toList();
    }

    @Override
    public Optional<Hymn> findById(UUID id) {
        return hymnJpaRepository.findById(id).map(HymnMapper::toDomain);
    }

    @Override
    public List<Hymn> findByIdIn(List<UUID> ids) {
        return hymnJpaRepository.findByIdIn(ids).stream().map(HymnMapper::toDomain).toList();
    }
}
