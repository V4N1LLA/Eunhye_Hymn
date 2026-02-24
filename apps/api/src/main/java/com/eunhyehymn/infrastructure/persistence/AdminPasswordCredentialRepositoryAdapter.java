package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.AdminPasswordCredential;
import com.eunhyehymn.domain.repository.AdminPasswordCredentialRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.AdminPasswordCredentialMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AdminPasswordCredentialRepositoryAdapter implements AdminPasswordCredentialRepository {
    private static final UUID CREDENTIAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final AdminPasswordCredentialJpaRepository jpaRepository;

    public AdminPasswordCredentialRepositoryAdapter(AdminPasswordCredentialJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<AdminPasswordCredential> find() {
        return jpaRepository.findById(CREDENTIAL_ID).map(AdminPasswordCredentialMapper::toDomain);
    }

    @Override
    public AdminPasswordCredential save(AdminPasswordCredential credential) {
        AdminPasswordCredential normalized = new AdminPasswordCredential(
            CREDENTIAL_ID,
            credential.loginId(),
            credential.passwordHash(),
            credential.createdAt(),
            credential.updatedAt()
        );
        return AdminPasswordCredentialMapper.toDomain(
            jpaRepository.save(AdminPasswordCredentialMapper.toEntity(normalized))
        );
    }
}
