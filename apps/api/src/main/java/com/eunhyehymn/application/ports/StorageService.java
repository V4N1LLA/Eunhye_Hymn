package com.eunhyehymn.application.ports;

import com.eunhyehymn.domain.model.AssetType;
import com.eunhyehymn.domain.model.PartType;
import java.time.Duration;
import java.util.UUID;

public interface StorageService {
    PresignResult presignUpload(PresignRequest request);

    default String resolveReadUrl(String objectKey, String fallbackUrl) {
        return fallbackUrl;
    }

    record PresignRequest(
        UUID hymnId,
        AssetType type,
        PartType part,
        String filename,
        String contentType,
        Duration expiresIn
    ) {
    }

    record PresignResult(
        String uploadUrl,
        String publicUrl,
        String objectKey
    ) {
    }
}
