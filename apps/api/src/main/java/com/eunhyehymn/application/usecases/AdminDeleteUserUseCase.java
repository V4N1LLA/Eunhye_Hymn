package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminDeleteUserUseCase {
    private final UserRepository userRepository;

    public AdminDeleteUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User delete(UUID requesterId, UUID targetUserId) {
        User existing = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "사용자를 찾을 수 없습니다", null));

        if (requesterId.equals(targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "self_delete", "자신의 계정은 삭제할 수 없습니다", null);
        }

        if (existing.role() == Role.ADMIN && existing.status() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "last_admin", "마지막 활성 관리자를 삭제할 수 없습니다", null);
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
