package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class AdminDeleteUserUseCase {
    private final UserRepository userRepository;

    public AdminDeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User delete(UUID requesterId, UUID targetUserId) {
        User existing = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));

        if (requesterId.equals(targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "self_delete", "cannot delete your own account", null);
        }

        if (existing.role() == Role.ADMIN && existing.status() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "last_admin",
                    "cannot delete the last active admin",
                    null
                );
            }
        }

        if (existing.status() == UserStatus.DISABLED) {
            return existing;
        }

        User deleted = new User(
            existing.id(),
            existing.displayName(),
            existing.role(),
            UserStatus.DISABLED,
            existing.createdAt(),
            existing.lastLoginAt()
        );
        return userRepository.save(deleted);
    }
}
