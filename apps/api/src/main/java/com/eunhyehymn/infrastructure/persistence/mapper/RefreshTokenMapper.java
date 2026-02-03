package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenEntity;

public final class RefreshTokenMapper {
    private RefreshTokenMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
            entity.getId(),
            entity.getUserId(),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getRevokedAt(),
            entity.getCreatedAt()
        );
    }

    public static RefreshTokenEntity toEntity(RefreshToken token) {
        return new RefreshTokenEntity(
            token.id(),
            token.userId(),
            token.tokenHash(),
            token.expiresAt(),
            token.revokedAt(),
            token.createdAt()
        );
    }
}
