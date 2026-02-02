package com.eunhyehymn.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHymnStateJpaRepository extends JpaRepository<UserHymnStateEntity, UserHymnStateId> {
}
