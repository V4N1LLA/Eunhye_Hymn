package com.eunhyehymn.infrastructure.storage;

import com.eunhyehymn.application.ports.StorageService;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3StorageService implements StorageService {
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicBaseUrl;
    private final String endpointOverride;
    private final Duration readPresignExpiresIn;

    public S3StorageService(
        S3Presigner presigner,
        String bucket,
        String publicBaseUrl,
        String endpointOverride,
        Duration readPresignExpiresIn
    ) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
        this.endpointOverride = endpointOverride;
        this.readPresignExpiresIn = readPresignExpiresIn;
    }

    @Override
    public PresignResult presignUpload(PresignRequest request) {
        String objectKey = buildObjectKey(
            request.hymnId(),
            request.type().name(),
            request.part() == null ? "ALL" : request.part().name(),
            request.filename()
        );

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(request.contentType())
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(request.expiresIn())
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        String publicUrl = resolvePublicUrl(objectKey);

        return new PresignResult(presigned.url().toString(), publicUrl, objectKey);
    }

    @Override
    public String resolveReadUrl(String objectKey, String fallbackUrl) {
        if (objectKey == null || objectKey.isBlank()) {
            return fallbackUrl;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(readPresignExpiresIn)
                .getObjectRequest(getObjectRequest)
                .build();
            PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception ignored) {
            return fallbackUrl;
        }
    }

    private String buildObjectKey(UUID hymnId, String type, String part, String filename) {
        String safeFilename = filename.trim().replace(" ", "_");
        String encodedFilename = URLEncoder.encode(safeFilename, StandardCharsets.UTF_8);
        return String.format("hymns/%s/%s/%s/%s-%s", hymnId, type, part, UUID.randomUUID(), encodedFilename);
    }

    private String resolvePublicUrl(String objectKey) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/$", "") + "/" + objectKey;
        }
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            String base = endpointOverride.replaceAll("/$", "");
            return base + "/" + bucket + "/" + objectKey;
        }
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, objectKey);
    }
}
