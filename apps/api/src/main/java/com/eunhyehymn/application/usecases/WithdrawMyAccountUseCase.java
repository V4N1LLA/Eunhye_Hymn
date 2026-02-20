package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class WithdrawMyAccountUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public WithdrawMyAccountUseCase(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void withdraw(UUID userId) {
        User existing = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));

        if (existing.role() == Role.ADMIN && existing.status() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "last_admin",
                    "cannot withdraw the last active admin",
                    null
                );
            }
        }

        Instant now = Instant.now();
        if (existing.status() != UserStatus.DISABLED) {
            User disabled = new User(
                existing.id(),
                existing.displayName(),
                existing.role(),
                UserStatus.DISABLED,
                existing.createdAt(),
                existing.lastLoginAt()
            );
            userRepository.save(disabled);
        }

        refreshTokenRepository.revokeActiveByUserId(userId, now);
    }
}
