package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.UserPasswordCredential;
import com.eunhyehymn.domain.repository.UserPasswordCredentialRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.UserPasswordCredentialMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserPasswordCredentialRepositoryAdapter implements UserPasswordCredentialRepository {
    private final UserPasswordCredentialJpaRepository jpaRepository;

    public UserPasswordCredentialRepositoryAdapter(UserPasswordCredentialJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserPasswordCredential> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(UserPasswordCredentialMapper::toDomain);
    }

    @Override
    public UserPasswordCredential save(UserPasswordCredential credential) {
        return UserPasswordCredentialMapper.toDomain(
            jpaRepository.save(UserPasswordCredentialMapper.toEntity(credential))
        );
    }
}
