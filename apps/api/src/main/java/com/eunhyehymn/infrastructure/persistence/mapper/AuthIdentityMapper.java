package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityEntity;

public final class AuthIdentityMapper {
    private AuthIdentityMapper() {
    }

    public static AuthIdentity toDomain(AuthIdentityEntity entity) {
        return new AuthIdentity(
            entity.getId(),
            entity.getUserId(),
            entity.getProvider(),
            entity.getProviderSubject(),
            entity.getEmail(),
            entity.getCreatedAt()
        );
    }

    public static AuthIdentityEntity toEntity(AuthIdentity identity) {
        return new AuthIdentityEntity(
            identity.id(),
            identity.userId(),
            identity.provider(),
            identity.providerSubject(),
            identity.email(),
            identity.createdAt()
        );
    }
}
