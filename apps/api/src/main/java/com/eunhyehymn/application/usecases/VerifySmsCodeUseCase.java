package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.SmsVerificationRequest;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.model.UserVerification;
import com.eunhyehymn.domain.repository.SmsVerificationRequestRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class VerifySmsCodeUseCase {
    private final SmsVerificationRequestRepository smsVerificationRequestRepository;
    private final UserRepository userRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final TokenHashService tokenHashService;
    private final int expectedCodeLength;
    private final int maxAttempts;

    public VerifySmsCodeUseCase(
        SmsVerificationRequestRepository smsVerificationRequestRepository,
        UserRepository userRepository,
        UserVerificationRepository userVerificationRepository,
        TokenHashService tokenHashService,
        int expectedCodeLength,
        int maxAttempts
    ) {
        if (expectedCodeLength <= 0 || expectedCodeLength > 9) {
            throw new IllegalArgumentException("SMS code length must be between 1 and 9.");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("SMS max attempts must be positive.");
        }
        this.smsVerificationRequestRepository = smsVerificationRequestRepository;
        this.userRepository = userRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.tokenHashService = tokenHashService;
        this.expectedCodeLength = expectedCodeLength;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public VerifyResult verify(UUID userId, UUID verificationId, String rawCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "User not found.", null));
        if (user.status() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "Disabled account.", null);
        }

        UserVerification userVerification = userVerificationRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "invite_not_verified",
                "Invite code verification is required first.",
                null
            ));
        if (!userVerification.isInviteVerified()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invite_not_verified",
                "Invite code verification is required first.",
                null
            );
        }

        SmsVerificationRequest request = smsVerificationRequestRepository.findById(verificationId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "sms_request_not_found",
                "Verification request not found.",
                null
            ));
        if (!request.userId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Verification request does not belong to user.", null);
        }

        if (request.isVerified()) {
            return new VerifyResult(true, userVerification.isCompleted());
        }

        Instant now = Instant.now();
        if (request.isExpired(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "sms_expired", "Verification code expired.", null);
        }
        if (request.attempts() >= maxAttempts) {
            throw new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "sms_attempts_exceeded",
                "Too many failed verification attempts.",
                null
            );
        }

        String normalizedCode = normalizeCode(rawCode, expectedCodeLength);
        String codeHash = tokenHashService.hash(normalizedCode);
        if (!codeHash.equals(request.codeHash())) {
            SmsVerificationRequest failed = new SmsVerificationRequest(
                request.id(),
                request.userId(),
                request.phoneNumber(),
                request.codeHash(),
                request.expiresAt(),
                request.attempts() + 1,
                request.verifiedAt(),
                request.createdAt(),
                now
            );
            smsVerificationRequestRepository.save(failed);
            return new VerifyResult(false, false);
        }

        SmsVerificationRequest verified = new SmsVerificationRequest(
            request.id(),
            request.userId(),
            request.phoneNumber(),
            request.codeHash(),
            request.expiresAt(),
            request.attempts(),
            now,
            request.createdAt(),
            now
        );
        smsVerificationRequestRepository.save(verified);

        UserVerification next = new UserVerification(
            userId,
            userVerification.inviteCode(),
            userVerification.inviteVerifiedAt(),
            request.phoneNumber(),
            now,
            now
        );
        userVerificationRepository.save(next);
        return new VerifyResult(true, next.isCompleted());
    }

    static String normalizeCode(String rawCode, int expectedCodeLength) {
        String normalized = rawCode == null ? "" : rawCode.replaceAll("\\D", "");
        if (normalized.length() != expectedCodeLength) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "sms_code_invalid",
                "Code length is invalid.",
                null
            );
        }
        return normalized;
    }

    public record VerifyResult(boolean verified, boolean completed) {
    }
}
