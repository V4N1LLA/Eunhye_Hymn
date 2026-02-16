package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.infrastructure.persistence.InviteCodeEntity;

public final class InviteCodeMapper {
    private InviteCodeMapper() {
    }

    public static InviteCode toDomain(InviteCodeEntity entity) {
        return new InviteCode(
            entity.getCode(),
            entity.getCreatedBy(),
            entity.getDescription(),
            entity.getMaxUses(),
            entity.getUsedCount(),
            entity.isEnabled(),
            entity.getExpiresAt(),
            entity.getCreatedAt()
        );
    }

    public static InviteCodeEntity toEntity(InviteCode model) {
        return new InviteCodeEntity(
            model.code(),
            model.createdBy(),
            model.description(),
            model.maxUses(),
            model.usedCount(),
            model.enabled(),
            model.expiresAt(),
            model.createdAt()
        );
    }
}
