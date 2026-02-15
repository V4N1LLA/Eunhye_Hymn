package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.AuthIdentity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository {
    AuthIdentity save(AuthIdentity identity);

    Optional<AuthIdentity> findById(UUID id);

    Optional<AuthIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);

    List<AuthIdentity> findByUserIdIn(List<UUID> userIds);
}
