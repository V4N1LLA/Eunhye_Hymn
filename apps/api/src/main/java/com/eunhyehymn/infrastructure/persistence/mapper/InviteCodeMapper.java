package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.infrastructure.persistence.InviteCodeEntity;

public final class InviteCodeMapper {
    private InviteCodeMapper() {
    }

    public static InviteCode toDomain(InviteCodeEntity entity) {
        return new InviteCode(
            entity.getId(),
            entity.getCode(),
            entity.getMaxUses(),
            entity.getUsedCount(),
            entity.getExpiresAt(),
            entity.getRevokedAt(),
            entity.getCreatedAt()
        );
    }

    public static InviteCodeEntity toEntity(InviteCode ic) {
        return new InviteCodeEntity(
            ic.id(),
            ic.code(),
            ic.maxUses(),
            ic.usedCount(),
            ic.expiresAt(),
            ic.revokedAt(),
            ic.createdAt()
        );
    }
}
