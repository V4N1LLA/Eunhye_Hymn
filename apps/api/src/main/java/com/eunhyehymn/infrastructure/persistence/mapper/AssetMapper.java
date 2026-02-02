package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.Asset;
import com.eunhyehymn.infrastructure.persistence.AssetEntity;

public final class AssetMapper {
    private AssetMapper() {
    }

    public static Asset toDomain(AssetEntity entity) {
        return new Asset(
            entity.getId(),
            entity.getHymnId(),
            entity.getType(),
            entity.getPart(),
            entity.getUrl(),
            entity.getChecksum(),
            entity.getVersion(),
            entity.getCreatedAt()
        );
    }

    public static AssetEntity toEntity(Asset asset) {
        return new AssetEntity(
            asset.id(),
            asset.hymnId(),
            asset.type(),
            asset.part(),
            asset.url(),
            asset.checksum(),
            asset.version(),
            asset.createdAt()
        );
    }
}
