package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserProfile;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.ProfileChangeRequestRepository;
import com.eunhyehymn.domain.repository.UserProfileRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class AdminReviewProfileChangeRequestUseCase {
    public enum Decision {
        APPROVE,
        REJECT
    }

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileChangeRequestRepository profileChangeRequestRepository;

    public AdminReviewProfileChangeRequestUseCase(
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        ProfileChangeRequestRepository profileChangeRequestRepository
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.profileChangeRequestRepository = profileChangeRequestRepository;
    }

    @Transactional
    public ProfileChangeRequest review(UUID reviewerId, UUID requestId, Decision decision, String rejectReason) {
        userRepository.findById(reviewerId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "reviewer not found", null));

        ProfileChangeRequest request = profileChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "profile_change_request_not_found",
                "profile change request not found",
                null
            ));
        if (request.status() != ProfileChangeRequestStatus.PENDING) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "profile_change_request_already_reviewed",
                "profile change request already reviewed",
                null
            );
        }

        User targetUser = userRepository.findById(request.userId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));
        if (targetUser.status() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "account disabled", null);
        }

        Instant now = Instant.now();
        if (decision == Decision.APPROVE) {
            userProfileRepository.save(new UserProfile(
                request.userId(),
                request.churchName(),
                request.name(),
                request.groupName(),
                request.gender(),
                now
            ));
            ProfileChangeRequest approved = new ProfileChangeRequest(
                request.id(),
                request.userId(),
                request.churchName(),
                request.name(),
                request.groupName(),
                request.gender(),
                ProfileChangeRequestStatus.APPROVED,
                request.requestedAt(),
                reviewerId,
                now,
                null
            );
            return profileChangeRequestRepository.save(approved);
        }

        String normalizedReason = normalizeReason(rejectReason);
        ProfileChangeRequest rejected = new ProfileChangeRequest(
            request.id(),
            request.userId(),
            request.churchName(),
            request.name(),
            request.groupName(),
            request.gender(),
            ProfileChangeRequestStatus.REJECTED,
            request.requestedAt(),
            reviewerId,
            now,
            normalizedReason
        );
        return profileChangeRequestRepository.save(rejected);
    }

    private String normalizeReason(String rejectReason) {
        if (rejectReason == null) {
            return null;
        }
        String normalized = rejectReason.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
