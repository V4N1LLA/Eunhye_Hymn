package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.RefreshTokenMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity saved = refreshTokenJpaRepository.save(RefreshTokenMapper.toEntity(token));
        return RefreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findById(UUID id) {
        return refreshTokenJpaRepository.findById(id).map(RefreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash).map(RefreshTokenMapper::toDomain);
    }
}
