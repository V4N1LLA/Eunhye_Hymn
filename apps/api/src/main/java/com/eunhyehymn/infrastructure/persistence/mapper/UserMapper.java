package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.infrastructure.persistence.UserEntity;

public final class UserMapper {
    private UserMapper() {
    }

    public static User toDomain(UserEntity entity) {
        return new User(
            entity.getId(),
            entity.getDisplayName(),
            entity.getRole(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getLastLoginAt()
        );
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
            user.id(),
            user.displayName(),
            user.role(),
            user.status(),
            user.createdAt(),
            user.lastLoginAt()
        );
    }
}
