package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class AdminUpdateUserUseCase {
    private final UserRepository userRepository;

    public AdminUpdateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User update(UUID requesterId, UUID targetUserId, Role role, UserStatus status) {
        if (role == null && status == null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "empty_update",
                "at least one field is required",
                null
            );
        }

        User existing = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));

        if (requesterId.equals(targetUserId) && role != null && role != existing.role()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "self_role_change",
                "cannot change your own role",
                null
            );
        }

        Role effectiveRole = role != null ? role : existing.role();
        UserStatus effectiveStatus = status != null ? status : existing.status();
        if (effectiveRole == existing.role() && effectiveStatus == existing.status()) {
            return existing;
        }

        boolean wouldLoseAdmin = existing.role() == Role.ADMIN
            && (effectiveRole != Role.ADMIN || effectiveStatus != UserStatus.ACTIVE);

        if (wouldLoseAdmin) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "last_admin",
                    "cannot update the last active admin",
                    null
                );
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
