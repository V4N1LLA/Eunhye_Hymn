package com.eunhyehymn.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsVerificationRequestJpaRepository extends JpaRepository<SmsVerificationRequestEntity, UUID> {
    Optional<SmsVerificationRequestEntity> findFirstByUserIdAndPhoneNumberOrderByCreatedAtDesc(UUID userId, String phoneNumber);

    void deleteByUserId(UUID userId);
}
