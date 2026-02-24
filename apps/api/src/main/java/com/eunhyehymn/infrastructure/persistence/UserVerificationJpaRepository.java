package com.eunhyehymn.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVerificationJpaRepository extends JpaRepository<UserVerificationEntity, UUID> {
}
