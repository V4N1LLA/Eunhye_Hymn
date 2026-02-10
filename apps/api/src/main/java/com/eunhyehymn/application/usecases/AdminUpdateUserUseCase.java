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
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "user_not_found", "사용자를 찾을 수 없습니다", null));
        Role newRole = role != null ? role : user.role();
        UserStatus newStatus = status != null ? status : user.status();
        User updated = new User(user.id(), user.displayName(), newRole, newStatus, user.createdAt(), user.lastLoginAt());
        return userRepository.save(updated);
    }
}
