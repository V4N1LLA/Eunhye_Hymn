package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.UserProfile;
import com.eunhyehymn.infrastructure.persistence.UserProfileEntity;

public final class UserProfileMapper {
    private UserProfileMapper() {
    }

    public static UserProfile toDomain(UserProfileEntity entity) {
        return new UserProfile(
            entity.getUserId(),
            entity.getChurchName(),
            entity.getMemberName(),
            entity.getGroupName(),
            entity.getGender(),
            entity.getUpdatedAt()
        );
    }

    public static UserProfileEntity toEntity(UserProfile profile) {
        return new UserProfileEntity(
            profile.userId(),
            profile.churchName(),
            profile.name(),
            profile.groupName(),
            profile.gender(),
            profile.updatedAt()
        );
    }
}
