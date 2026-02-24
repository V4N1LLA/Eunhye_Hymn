package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Gender;
import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.ProfileChangeRequestRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class RequestMyProfileChangeUseCase {
    private static final int MAX_TEXT_LENGTH = 255;

    private final UserRepository userRepository;
    private final ProfileChangeRequestRepository profileChangeRequestRepository;

    public RequestMyProfileChangeUseCase(
        UserRepository userRepository,
        ProfileChangeRequestRepository profileChangeRequestRepository
    ) {
        this.userRepository = userRepository;
        this.profileChangeRequestRepository = profileChangeRequestRepository;
    }

    public ProfileChangeRequest request(
        UUID userId,
        String churchName,
        String name,
        String groupName,
        String gender
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));
        if (user.status() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "account disabled", null);
        }

        String normalizedChurchName = normalizeRequired(churchName, "churchName");
        String normalizedName = normalizeRequired(name, "name");
        String normalizedGroupName = normalizeRequired(groupName, "group");
        Gender normalizedGender = normalizeGender(gender);

        Instant now = Instant.now();
        ProfileChangeRequest pending = profileChangeRequestRepository.findPendingByUserId(userId).orElse(null);
        ProfileChangeRequest next = new ProfileChangeRequest(
            pending == null ? UUID.randomUUID() : pending.id(),
            userId,
            normalizedChurchName,
            normalizedName,
            normalizedGroupName,
            normalizedGender,
            ProfileChangeRequestStatus.PENDING,
            now,
            null,
            null,
            null
        );
        return profileChangeRequestRepository.save(next);
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                fieldName + " is required",
                null
            );
        }
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                fieldName + " is too long",
                null
            );
        }
        return normalized;
    }

    private Gender normalizeGender(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Gender.UNKNOWN;
        }
        try {
            return Gender.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "invalid gender",
                null
            );
        }
    }
}
