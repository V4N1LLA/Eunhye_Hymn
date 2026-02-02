package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.Asset;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository {
    Asset save(Asset asset);

    Optional<Asset> findById(UUID id);
}
