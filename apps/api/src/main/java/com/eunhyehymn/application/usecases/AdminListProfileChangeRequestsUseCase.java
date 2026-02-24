package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import com.eunhyehymn.domain.repository.ProfileChangeRequestRepository;
import java.util.List;

public class AdminListProfileChangeRequestsUseCase {
    private final ProfileChangeRequestRepository profileChangeRequestRepository;

    public AdminListProfileChangeRequestsUseCase(ProfileChangeRequestRepository profileChangeRequestRepository) {
        this.profileChangeRequestRepository = profileChangeRequestRepository;
    }

    public List<ProfileChangeRequest> list(ProfileChangeRequestStatus status) {
        ProfileChangeRequestStatus normalizedStatus = status == null ? ProfileChangeRequestStatus.PENDING : status;
        return profileChangeRequestRepository.findByStatus(normalizedStatus);
    }
}
