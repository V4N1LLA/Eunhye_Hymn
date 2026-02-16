package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthIdentityJpaRepository extends JpaRepository<AuthIdentityEntity, UUID> {
    Optional<AuthIdentityEntity> findByProviderAndProviderSubject(String provider, String providerSubject);

    List<AuthIdentityEntity> findByUserIdIn(List<UUID> userIds);

    Optional<AuthIdentityEntity> findByUserIdAndProvider(UUID userId, String provider);
}
