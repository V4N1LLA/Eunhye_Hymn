package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserProfile;
import com.eunhyehymn.domain.repository.UserProfileRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class GetMyProfileUseCase {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public GetMyProfileUseCase(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public Result get(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        return new Result(
            user.id(),
            user.role(),
            user.displayName(),
            profile == null ? null : profile.churchName(),
            profile == null ? null : profile.name(),
            profile == null ? null : profile.groupName(),
            profile == null ? null : profile.gender().name(),
            profile == null ? null : profile.updatedAt()
        );
    }

    public record Result(
        UUID userId,
        Role role,
        String displayName,
        String churchName,
        String name,
        String group,
        String gender,
        Instant profileUpdatedAt
    ) {
        public boolean profileCompleted() {
            return churchName != null && !churchName.isBlank()
                && name != null && !name.isBlank()
                && group != null && !group.isBlank();
        }
    }
}
