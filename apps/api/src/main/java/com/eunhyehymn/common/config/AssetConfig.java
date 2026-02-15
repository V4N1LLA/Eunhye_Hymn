package com.eunhyehymn.common.config;

import com.eunhyehymn.application.ports.StorageService;
import com.eunhyehymn.application.usecases.AdminConfirmAssetUseCase;
import com.eunhyehymn.application.usecases.AdminDeleteAssetUseCase;
import com.eunhyehymn.application.usecases.AdminPresignAssetUseCase;
import com.eunhyehymn.domain.repository.AssetRepository;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.infrastructure.storage.S3StorageService;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AssetConfig {
    @Bean
    StorageService storageService(
        @Value("${storage.s3.bucket:local-bucket}") String bucket,
        @Value("${storage.s3.region:ap-northeast-2}") String region,
        @Value("${storage.s3.endpoint:}") String endpoint,
        @Value("${storage.s3.public-base-url:}") String publicBaseUrl,
        @Value("${storage.s3.read-presign-expires-minutes:720}") long readPresignExpiresMinutes
    ) {
        S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return new S3StorageService(
            builder.build(),
            bucket,
            publicBaseUrl,
            endpoint,
            Duration.ofMinutes(readPresignExpiresMinutes)
        );
    }

    @Bean
    AdminPresignAssetUseCase adminPresignAssetUseCase(
        StorageService storageService,
        HymnRepository hymnRepository,
        @Value("${storage.s3.presign-expires-minutes:15}") long presignExpiresMinutes
    ) {
        return new AdminPresignAssetUseCase(storageService, hymnRepository, Duration.ofMinutes(presignExpiresMinutes));
    }

    @Bean
    AdminConfirmAssetUseCase adminConfirmAssetUseCase(
        HymnRepository hymnRepository,
        AssetRepository assetRepository
    ) {
        return new AdminConfirmAssetUseCase(hymnRepository, assetRepository);
    }

    @Bean
    AdminDeleteAssetUseCase adminDeleteAssetUseCase(AssetRepository assetRepository) {
        return new AdminDeleteAssetUseCase(assetRepository);
    }
}
