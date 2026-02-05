package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Asset;
import com.eunhyehymn.domain.model.AssetType;
import com.eunhyehymn.domain.model.PartType;
import com.eunhyehymn.domain.repository.AssetRepository;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminConfirmAssetUseCase {
    private final HymnRepository hymnRepository;
    private final AssetRepository assetRepository;

    public AdminConfirmAssetUseCase(HymnRepository hymnRepository, AssetRepository assetRepository) {
        this.hymnRepository = hymnRepository;
        this.assetRepository = assetRepository;
    }

    public Asset confirm(
        UUID hymnId,
        AssetType type,
        PartType part,
        String publicUrl,
        String objectKey,
        String checksum,
        String version
    ) {
        PartType resolvedPart = part == null ? PartType.ALL : part;
        String requiredPrefix = String.format("hymns/%s/%s/%s/", hymnId, type, resolvedPart);
        if (!objectKey.startsWith(requiredPrefix)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_object_key",
                "objectKey는 hymns/{hymnId}/{type}/{part}/ 형식을 따라야 합니다",
                null
            );
        }
        if (!hymnRepository.findById(hymnId).isPresent()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "hymn_not_found", "찬송가를 찾을 수 없습니다", null);
        }

        assetRepository.deleteByHymnIdAndTypeAndPart(hymnId, type, resolvedPart);

        Asset asset = new Asset(
            UUID.randomUUID(),
            hymnId,
            type,
            resolvedPart,
            publicUrl,
            objectKey,
            checksum,
            version,
            Instant.now()
        );
        return assetRepository.save(asset);
    }
}
