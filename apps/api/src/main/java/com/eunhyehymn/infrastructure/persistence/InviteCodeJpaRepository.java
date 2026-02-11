package com.eunhyehymn.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteCodeJpaRepository extends JpaRepository<InviteCodeEntity, String> {
}
