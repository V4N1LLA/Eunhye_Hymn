package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.UserVerification;
import java.util.Optional;
import java.util.UUID;

public interface UserVerificationRepository {
    Optional<UserVerification> findByUserId(UUID userId);

    UserVerification save(UserVerification verification);

    void deleteByUserId(UUID userId);
}
