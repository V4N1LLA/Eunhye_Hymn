package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface UserHymnStateJpaRepository extends JpaRepository<UserHymnStateEntity, UserHymnStateId> {
    Optional<UserHymnStateEntity> findByIdUserIdAndIdHymnId(UUID userId, UUID hymnId);

    List<UserHymnStateEntity> findByIdUserIdOrderByLastOpenedAtDesc(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByIdHymnId(UUID hymnId);
}
