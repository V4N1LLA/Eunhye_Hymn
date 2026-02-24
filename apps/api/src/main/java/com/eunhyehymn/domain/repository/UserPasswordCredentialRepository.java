package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.UserPasswordCredential;
import java.util.Optional;
import java.util.UUID;

public interface UserPasswordCredentialRepository {
    Optional<UserPasswordCredential> findByUserId(UUID userId);

    UserPasswordCredential save(UserPasswordCredential credential);
}
