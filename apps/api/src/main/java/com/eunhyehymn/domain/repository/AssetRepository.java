package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.Asset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository {
    Asset save(Asset asset);

    Optional<Asset> findById(UUID id);

    List<Asset> findByHymnId(UUID hymnId);

    void deleteById(UUID id);

    void deleteByHymnId(UUID hymnId);

    void deleteByHymnIdAndTypeAndPart(UUID hymnId, com.eunhyehymn.domain.model.AssetType type, com.eunhyehymn.domain.model.PartType part);
}
