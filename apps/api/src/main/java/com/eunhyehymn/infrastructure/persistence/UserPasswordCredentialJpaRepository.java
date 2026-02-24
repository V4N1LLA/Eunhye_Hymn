package com.eunhyehymn.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPasswordCredentialJpaRepository
    extends JpaRepository<UserPasswordCredentialEntity, UUID> {
}
