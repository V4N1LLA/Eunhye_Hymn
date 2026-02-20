package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.SmsVerificationRequest;
import java.util.Optional;
import java.util.UUID;

public interface SmsVerificationRequestRepository {
    SmsVerificationRequest save(SmsVerificationRequest request);

    Optional<SmsVerificationRequest> findById(UUID id);

    Optional<SmsVerificationRequest> findLatestByUserIdAndPhoneNumber(UUID userId, String phoneNumber);

    void deleteByUserId(UUID userId);
}
