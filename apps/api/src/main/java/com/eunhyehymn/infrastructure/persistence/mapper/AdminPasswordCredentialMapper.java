package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.AdminPasswordCredential;
import com.eunhyehymn.infrastructure.persistence.AdminPasswordCredentialEntity;

public final class AdminPasswordCredentialMapper {
    private AdminPasswordCredentialMapper() {
    }

    public static AdminPasswordCredential toDomain(AdminPasswordCredentialEntity entity) {
        return new AdminPasswordCredential(
            entity.getId(),
            entity.getLoginId(),
            entity.getPasswordHash(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static AdminPasswordCredentialEntity toEntity(AdminPasswordCredential credential) {
        return new AdminPasswordCredentialEntity(
            credential.id(),
            credential.loginId(),
            credential.passwordHash(),
            credential.createdAt(),
            credential.updatedAt()
        );
    }
}
