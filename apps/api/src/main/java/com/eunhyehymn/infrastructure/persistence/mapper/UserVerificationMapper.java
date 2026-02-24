package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.UserVerification;
import com.eunhyehymn.infrastructure.persistence.UserVerificationEntity;

public final class UserVerificationMapper {
    private UserVerificationMapper() {
    }

    public static UserVerification toDomain(UserVerificationEntity entity) {
        return new UserVerification(
            entity.getUserId(),
            entity.getInviteCode(),
            entity.getInviteVerifiedAt(),
            entity.getPhoneNumber(),
            entity.getPhoneVerifiedAt(),
            entity.getUpdatedAt()
        );
    }

    public static UserVerificationEntity toEntity(UserVerification verification) {
        return new UserVerificationEntity(
            verification.userId(),
            verification.inviteCode(),
            verification.inviteVerifiedAt(),
            verification.phoneNumber(),
            verification.phoneVerifiedAt(),
            verification.updatedAt()
        );
    }
}
