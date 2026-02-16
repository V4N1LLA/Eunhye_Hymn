package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.InviteCodeMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InviteCodeRepositoryAdapter implements InviteCodeRepository {
    private final InviteCodeJpaRepository jpaRepository;

    public InviteCodeRepositoryAdapter(InviteCodeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public InviteCode save(InviteCode inviteCode) {
        InviteCodeEntity saved = jpaRepository.save(InviteCodeMapper.toEntity(inviteCode));
        return InviteCodeMapper.toDomain(saved);
    }

    @Override
    public Optional<InviteCode> findByCode(String code) {
        return jpaRepository.findById(code).map(InviteCodeMapper::toDomain);
    }

    @Override
    public List<InviteCode> findAll() {
        return jpaRepository.findAll().stream()
            .map(InviteCodeMapper::toDomain)
            .toList();
    }

    @Override
    public boolean incrementUsedCount(String code) {
        return jpaRepository.incrementUsedCount(code) > 0;
    }
}
