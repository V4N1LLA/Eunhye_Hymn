package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.model.UserVerification;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmInviteCodeUseCase {
    private final ValidateInviteCodeUseCase validateInviteCodeUseCase;
    private final InviteCodeRepository inviteCodeRepository;
    private final UserRepository userRepository;
    private final UserVerificationRepository userVerificationRepository;

    public ConfirmInviteCodeUseCase(
        ValidateInviteCodeUseCase validateInviteCodeUseCase,
        InviteCodeRepository inviteCodeRepository,
        UserRepository userRepository,
        UserVerificationRepository userVerificationRepository
    ) {
        this.validateInviteCodeUseCase = validateInviteCodeUseCase;
        this.inviteCodeRepository = inviteCodeRepository;
        this.userRepository = userRepository;
        this.userVerificationRepository = userVerificationRepository;
    }

    @Transactional
    public boolean confirm(UUID userId, String inviteCode) {
        String normalizedCode = normalizeInviteCode(inviteCode);
        if (normalizedCode == null || normalizedCode.isBlank()) {
            return false;
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "User not found.", null));
        if (user.status() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "Disabled account.", null);
        }

        UserVerification existing = userVerificationRepository.findByUserId(userId).orElse(null);
        if (existing != null && existing.isInviteVerified()) {
            return existing.inviteCode() != null && existing.inviteCode().equalsIgnoreCase(normalizedCode);
        }

        if (!validateInviteCodeUseCase.validate(normalizedCode)) {
            return false;
        }

        if (!inviteCodeRepository.incrementUsedCount(normalizedCode)) {
            return false;
        }

        Instant now = Instant.now();
        UserVerification next = existing == null
            ? new UserVerification(userId, normalizedCode, now, null, null, now)
            : new UserVerification(
                userId,
                normalizedCode,
                now,
                existing.phoneNumber(),
                existing.phoneVerifiedAt(),
                now
            );
        userVerificationRepository.save(next);
        return true;
    }

    static String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null) {
            return null;
        }
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
