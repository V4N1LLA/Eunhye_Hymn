package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHymnStateJpaRepository extends JpaRepository<UserHymnStateEntity, UserHymnStateId> {
    Optional<UserHymnStateEntity> findByIdUserIdAndIdHymnId(UUID userId, UUID hymnId);

    List<UserHymnStateEntity> findByIdUserIdOrderByLastOpenedAtDesc(UUID userId);
}
