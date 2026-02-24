package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.SmsSender;
import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.SmsVerificationRequest;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.model.UserVerification;
import com.eunhyehymn.domain.repository.SmsVerificationRequestRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class RequestSmsCodeUseCase {
    private final SmsVerificationRequestRepository smsVerificationRequestRepository;
    private final UserRepository userRepository;
    private final UserVerificationRepository userVerificationRepository;
    private final TokenHashService tokenHashService;
    private final SmsSender smsSender;
    private final int codeLength;
    private final long expiresSeconds;
    private final long cooldownSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RequestSmsCodeUseCase(
        SmsVerificationRequestRepository smsVerificationRequestRepository,
        UserRepository userRepository,
        UserVerificationRepository userVerificationRepository,
        TokenHashService tokenHashService,
        SmsSender smsSender,
        int codeLength,
        long expiresSeconds,
        long cooldownSeconds
    ) {
        if (codeLength <= 0 || codeLength > 9) {
            throw new IllegalArgumentException("SMS code length must be between 1 and 9.");
        }
        this.smsVerificationRequestRepository = smsVerificationRequestRepository;
        this.userRepository = userRepository;
        this.userVerificationRepository = userVerificationRepository;
        this.tokenHashService = tokenHashService;
        this.smsSender = smsSender;
        this.codeLength = codeLength;
        this.expiresSeconds = expiresSeconds;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Transactional
    public RequestResult request(UUID userId, String rawPhoneNumber) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "User not found.", null));
        if (user.status() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "Disabled account.", null);
        }

        UserVerification verification = userVerificationRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "invite_not_verified",
                "Invite code verification is required first.",
                null
            ));
        if (!verification.isInviteVerified()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invite_not_verified",
                "Invite code verification is required first.",
                null
            );
        }

        String phoneNumber = normalizePhoneNumber(rawPhoneNumber);
        String destination = toE164(phoneNumber);
        Instant now = Instant.now();

        smsVerificationRequestRepository.findLatestByUserIdAndPhoneNumber(userId, phoneNumber)
            .ifPresent(previous -> {
                Instant availableAt = previous.createdAt().plusSeconds(cooldownSeconds);
                if (availableAt.isAfter(now)) {
                    long secondsLeft = Duration.between(now, availableAt).toSeconds();
                    throw new ApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "sms_cooldown",
                        "Please wait before requesting another code.",
                        Map.of("secondsLeft", Math.max(secondsLeft, 1))
                    );
                }
            });

        String code = generateCode(codeLength);
        SmsVerificationRequest request = new SmsVerificationRequest(
            UUID.randomUUID(),
            userId,
            phoneNumber,
            tokenHashService.hash(code),
            now.plusSeconds(expiresSeconds),
            0,
            null,
            now,
            now
        );
        smsVerificationRequestRepository.save(request);
        smsSender.sendVerificationCode(destination, code);

        return new RequestResult(request.id().toString(), expiresSeconds, cooldownSeconds);
    }

    static String normalizePhoneNumber(String rawPhoneNumber) {
        String normalized = rawPhoneNumber == null ? "" : rawPhoneNumber.replaceAll("\\D", "");
        if (normalized.length() < 10 || normalized.length() > 11) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "sms_phone_invalid",
                "Phone number must be 10-11 digits.",
                null
            );
        }
        return normalized;
    }

    static String toE164(String phoneNumber) {
        if (phoneNumber.startsWith("0")) {
            return "+82" + phoneNumber.substring(1);
        }
        if (phoneNumber.startsWith("82")) {
            return "+" + phoneNumber;
        }
        return "+" + phoneNumber;
    }

    private String generateCode(int length) {
        int bound = (int) Math.pow(10, length);
        int value = secureRandom.nextInt(bound);
        return String.format("%0" + length + "d", value);
    }

    public record RequestResult(String verificationId, long expiresInSeconds, long cooldownSeconds) {
    }
}
