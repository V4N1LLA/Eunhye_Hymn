package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.SmsVerificationRequest;
import com.eunhyehymn.infrastructure.persistence.SmsVerificationRequestEntity;

public final class SmsVerificationRequestMapper {
    private SmsVerificationRequestMapper() {
    }

    public static SmsVerificationRequest toDomain(SmsVerificationRequestEntity entity) {
        return new SmsVerificationRequest(
            entity.getId(),
            entity.getUserId(),
            entity.getPhoneNumber(),
            entity.getCodeHash(),
            entity.getExpiresAt(),
            entity.getAttempts(),
            entity.getVerifiedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static SmsVerificationRequestEntity toEntity(SmsVerificationRequest request) {
        return new SmsVerificationRequestEntity(
            request.id(),
            request.userId(),
            request.phoneNumber(),
            request.codeHash(),
            request.expiresAt(),
            request.attempts(),
            request.verifiedAt(),
            request.createdAt(),
            request.updatedAt()
        );
    }
}
