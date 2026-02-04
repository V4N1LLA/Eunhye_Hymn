package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.Asset;
import com.eunhyehymn.domain.repository.AssetRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.AssetMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AssetRepositoryAdapter implements AssetRepository {
    private final AssetJpaRepository assetJpaRepository;

    public AssetRepositoryAdapter(AssetJpaRepository assetJpaRepository) {
        this.assetJpaRepository = assetJpaRepository;
    }

    @Override
    public Asset save(Asset asset) {
        AssetEntity saved = assetJpaRepository.save(AssetMapper.toEntity(asset));
        return AssetMapper.toDomain(saved);
    }

    @Override
    public Optional<Asset> findById(UUID id) {
        return assetJpaRepository.findById(id).map(AssetMapper::toDomain);
    }

    @Override
    public List<Asset> findByHymnId(UUID hymnId) {
        return assetJpaRepository.findByHymnId(hymnId).stream().map(AssetMapper::toDomain).toList();
    }

    @Override
    public void deleteByHymnIdAndTypeAndPart(UUID hymnId, com.eunhyehymn.domain.model.AssetType type, com.eunhyehymn.domain.model.PartType part) {
        assetJpaRepository.deleteByHymnIdAndTypeAndPart(hymnId, type, part);
    }
}
