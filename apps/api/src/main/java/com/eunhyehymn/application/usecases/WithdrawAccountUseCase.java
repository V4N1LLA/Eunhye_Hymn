package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.SmsVerificationRequestRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class WithdrawAccountUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final SmsVerificationRequestRepository smsVerificationRequestRepository;

    public WithdrawAccountUseCase(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        UserVerificationRepository userVerificationRepository,
        SmsVerificationRequestRepository smsVerificationRequestRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.smsVerificationRequestRepository = smsVerificationRequestRepository;
    }

    @Transactional
    public void withdraw(UUID userId) {
        User existing = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "User not found.", null));

        if (existing.role() == Role.ADMIN && existing.status() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "last_admin", "Cannot disable the last active admin.", null);
            }
        }

        Instant now = Instant.now();
        if (existing.status() == UserStatus.ACTIVE) {
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

        refreshTokenRepository.revokeAllByUserId(userId, now);
        smsVerificationRequestRepository.deleteByUserId(userId);
        userVerificationRepository.deleteByUserId(userId);
    }
}
