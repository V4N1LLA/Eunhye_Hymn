package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HymnJpaRepository extends JpaRepository<HymnEntity, UUID> {
    List<HymnEntity> findByEnabledTrue();
}
