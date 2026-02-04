package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.StorageService;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.AssetType;
import com.eunhyehymn.domain.model.PartType;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminPresignAssetUseCase {
    private final StorageService storageService;
    private final HymnRepository hymnRepository;
    private final Duration expiresIn;

    public AdminPresignAssetUseCase(
        StorageService storageService,
        HymnRepository hymnRepository,
        Duration expiresIn
    ) {
        this.storageService = storageService;
        this.hymnRepository = hymnRepository;
        this.expiresIn = expiresIn;
    }

    public StorageService.PresignResult presign(
        UUID hymnId,
        AssetType type,
        PartType part,
        String filename,
        String contentType
    ) {
        if (!hymnRepository.findById(hymnId).isPresent()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "hymn_not_found", "찬송가를 찾을 수 없습니다", null);
        }

        StorageService.PresignRequest request = new StorageService.PresignRequest(
            hymnId,
            type,
            part,
            filename,
            contentType,
            expiresIn
        );
        return storageService.presignUpload(request);
    }
}
