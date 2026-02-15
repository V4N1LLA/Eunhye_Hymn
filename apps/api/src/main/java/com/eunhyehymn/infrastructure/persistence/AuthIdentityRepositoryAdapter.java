package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.AuthIdentityMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AuthIdentityRepositoryAdapter implements AuthIdentityRepository {
    private final AuthIdentityJpaRepository authIdentityJpaRepository;

    public AuthIdentityRepositoryAdapter(AuthIdentityJpaRepository authIdentityJpaRepository) {
        this.authIdentityJpaRepository = authIdentityJpaRepository;
    }

    @Override
    public AuthIdentity save(AuthIdentity identity) {
        AuthIdentityEntity saved = authIdentityJpaRepository.save(AuthIdentityMapper.toEntity(identity));
        return AuthIdentityMapper.toDomain(saved);
    }

    @Override
    public Optional<AuthIdentity> findById(UUID id) {
        return authIdentityJpaRepository.findById(id).map(AuthIdentityMapper::toDomain);
    }

    @Override
    public Optional<AuthIdentity> findByProviderAndProviderSubject(String provider, String providerSubject) {
        return authIdentityJpaRepository.findByProviderAndProviderSubject(provider, providerSubject)
            .map(AuthIdentityMapper::toDomain);
    }

    @Override
    public List<AuthIdentity> findByUserIdIn(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return authIdentityJpaRepository.findByUserIdIn(userIds).stream()
            .map(AuthIdentityMapper::toDomain)
            .toList();
    }
}
