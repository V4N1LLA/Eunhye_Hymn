package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileChangeRequestRepository {
    ProfileChangeRequest save(ProfileChangeRequest request);

    Optional<ProfileChangeRequest> findById(UUID id);

    Optional<ProfileChangeRequest> findPendingByUserId(UUID userId);

    Optional<ProfileChangeRequest> findLatestByUserId(UUID userId);

    List<ProfileChangeRequest> findByStatus(ProfileChangeRequestStatus status);
}
