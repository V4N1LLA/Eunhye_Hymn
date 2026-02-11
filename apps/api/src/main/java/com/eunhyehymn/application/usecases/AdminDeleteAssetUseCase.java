package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.repository.AssetRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminDeleteAssetUseCase {
    private final AssetRepository assetRepository;

    public AdminDeleteAssetUseCase(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public void delete(UUID assetId) {
        assetRepository.findById(assetId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "asset_not_found",
                "에셋을 찾을 수 없습니다: " + assetId,
                null
            ));
        assetRepository.deleteById(assetId);
    }
}
