package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.repository.ProfileChangeRequestRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class GetLatestMyProfileChangeRequestUseCase {
    private final UserRepository userRepository;
    private final ProfileChangeRequestRepository profileChangeRequestRepository;

    public GetLatestMyProfileChangeRequestUseCase(
        UserRepository userRepository,
        ProfileChangeRequestRepository profileChangeRequestRepository
    ) {
        this.userRepository = userRepository;
        this.profileChangeRequestRepository = profileChangeRequestRepository;
    }

    public ProfileChangeRequest get(UUID userId) {
        userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));

        return profileChangeRequestRepository.findLatestByUserId(userId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "profile_change_request_not_found",
                "profile change request not found",
                null
            ));
    }
}
