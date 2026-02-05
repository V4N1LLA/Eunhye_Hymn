package com.eunhyehymn.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetJpaRepository extends JpaRepository<AssetEntity, UUID> {
    List<AssetEntity> findByHymnId(UUID hymnId);

    void deleteByHymnIdAndTypeAndPart(UUID hymnId, com.eunhyehymn.domain.model.AssetType type, com.eunhyehymn.domain.model.PartType part);
}
