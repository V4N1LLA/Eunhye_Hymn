package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.infrastructure.persistence.ProfileChangeRequestEntity;

public final class ProfileChangeRequestMapper {
    private ProfileChangeRequestMapper() {
    }

    public static ProfileChangeRequest toDomain(ProfileChangeRequestEntity entity) {
        return new ProfileChangeRequest(
            entity.getId(),
            entity.getUserId(),
            entity.getChurchName(),
            entity.getMemberName(),
            entity.getGroupName(),
            entity.getGender(),
            entity.getStatus(),
            entity.getRequestedAt(),
            entity.getReviewedBy(),
            entity.getReviewedAt(),
            entity.getRejectReason()
        );
    }

    public static ProfileChangeRequestEntity toEntity(ProfileChangeRequest request) {
        return new ProfileChangeRequestEntity(
            request.id(),
            request.userId(),
            request.churchName(),
            request.name(),
            request.groupName(),
            request.gender(),
            request.status(),
            request.requestedAt(),
            request.reviewedBy(),
            request.reviewedAt(),
            request.rejectReason()
        );
    }
}
