package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVerificationJpaRepository extends JpaRepository<UserVerificationEntity, UUID> {
    List<UserVerificationEntity> findByUserIdIn(List<UUID> userIds);
}
