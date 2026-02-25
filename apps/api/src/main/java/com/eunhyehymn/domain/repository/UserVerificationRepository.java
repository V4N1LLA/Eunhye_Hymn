package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.UserVerification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserVerificationRepository {
    Optional<UserVerification> findByUserId(UUID userId);

    List<UserVerification> findByUserIdIn(List<UUID> userIds);

    UserVerification save(UserVerification verification);

    void deleteByUserId(UUID userId);
}
