package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminUpdateUserUseCase {
    private final UserRepository userRepository;

    public AdminUpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User update(UUID userId, Role role, UserStatus status) {
        User existing = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "사용자를 찾을 수 없습니다", null));

        User updated = new User(
            existing.id(),
            existing.displayName(),
            role != null ? role : existing.role(),
            status != null ? status : existing.status(),
            existing.createdAt(),
            existing.lastLoginAt()
        );

        return userRepository.save(updated);
    }
}
