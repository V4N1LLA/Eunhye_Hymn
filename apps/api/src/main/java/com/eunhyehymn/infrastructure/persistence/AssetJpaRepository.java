package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface AssetJpaRepository extends JpaRepository<AssetEntity, UUID> {
    List<AssetEntity> findByHymnIdOrderByCreatedAtAscIdAsc(UUID hymnId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByHymnId(UUID hymnId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    void deleteByHymnIdAndTypeAndPart(UUID hymnId, com.eunhyehymn.domain.model.AssetType type, com.eunhyehymn.domain.model.PartType part);
}
