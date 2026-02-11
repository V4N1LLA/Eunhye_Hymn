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

    public User update(UUID requesterId, UUID targetUserId, Role role, UserStatus status) {
        User existing = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "사용자를 찾을 수 없습니다", null));

        if (requesterId.equals(targetUserId) && role != null && role != existing.role()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "self_role_change", "자신의 역할은 변경할 수 없습니다", null);
        }

        Role effectiveRole = role != null ? role : existing.role();
        UserStatus effectiveStatus = status != null ? status : existing.status();
        boolean wouldLoseAdmin = existing.role() == Role.ADMIN
            && (effectiveRole != Role.ADMIN || effectiveStatus != UserStatus.ACTIVE);

        if (wouldLoseAdmin) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "last_admin", "마지막 활성 관리자를 변경할 수 없습니다", null);
            }
        }

        User updated = new User(
            existing.id(),
            existing.displayName(),
            effectiveRole,
            effectiveStatus,
            existing.createdAt(),
            existing.lastLoginAt()
        );

        return userRepository.save(updated);
    }
}
