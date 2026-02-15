package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminCreateUserUseCase {
    private final UserRepository userRepository;

    public AdminCreateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String displayName, Role role, UserStatus status) {
        String resolvedDisplayName = displayName == null ? "" : displayName.trim();
        if (resolvedDisplayName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "displayName은 비워둘 수 없습니다", null);
        }

        Role resolvedRole = role != null ? role : Role.USER;
        UserStatus resolvedStatus = status != null ? status : UserStatus.ACTIVE;
        Instant now = Instant.now();

        User user = new User(
            UUID.randomUUID(),
            resolvedDisplayName,
            resolvedRole,
            resolvedStatus,
            now,
            null
        );
        return userRepository.save(user);
    }
}
