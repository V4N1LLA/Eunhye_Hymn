package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, UUID> {
    List<UserProfileEntity> findByUserIdIn(List<UUID> userIds);
}
