package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.UserPasswordCredential;
import com.eunhyehymn.infrastructure.persistence.UserPasswordCredentialEntity;

public final class UserPasswordCredentialMapper {
    private UserPasswordCredentialMapper() {
    }

    public static UserPasswordCredential toDomain(UserPasswordCredentialEntity entity) {
        return new UserPasswordCredential(
            entity.getUserId(),
            entity.getPasswordHash(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static UserPasswordCredentialEntity toEntity(UserPasswordCredential credential) {
        return new UserPasswordCredentialEntity(
            credential.userId(),
            credential.passwordHash(),
            credential.createdAt(),
            credential.updatedAt()
        );
    }
}
