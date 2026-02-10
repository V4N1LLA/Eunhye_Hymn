package com.eunhyehymn.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteCodeJpaRepository extends JpaRepository<InviteCodeEntity, UUID> {
    Optional<InviteCodeEntity> findByCode(String code);
}
