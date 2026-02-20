package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.UserProfile;
import com.eunhyehymn.domain.repository.UserProfileRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UpsertMyProfileUseCase {
    private static final int MAX_TEXT_LENGTH = 255;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UpsertMyProfileUseCase(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile upsert(UUID userId, String churchName, String name, String group) {
        userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));

        String normalizedChurchName = normalizeRequired(churchName, "churchName");
        String normalizedName = normalizeRequired(name, "name");
        String normalizedGroup = normalizeRequired(group, "group");

        UserProfile profile = new UserProfile(
            userId,
            normalizedChurchName,
            normalizedName,
            normalizedGroup,
            Instant.now()
        );
        return userProfileRepository.save(profile);
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
}
