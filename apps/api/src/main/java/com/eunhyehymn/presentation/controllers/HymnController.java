package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.GetHymnDetailUseCase;
import com.eunhyehymn.application.usecases.ListHymnsUseCase;
import com.eunhyehymn.application.ports.StorageService;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.Asset;
import com.eunhyehymn.domain.model.Hymn;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hymns")
public class HymnController {
    private final ListHymnsUseCase listHymnsUseCase;
    private final GetHymnDetailUseCase getHymnDetailUseCase;
    private final StorageService storageService;

    public HymnController(
        ListHymnsUseCase listHymnsUseCase,
        GetHymnDetailUseCase getHymnDetailUseCase,
        StorageService storageService
    ) {
        this.listHymnsUseCase = listHymnsUseCase;
        this.getHymnDetailUseCase = getHymnDetailUseCase;
        this.storageService = storageService;
    }

    @GetMapping
    public ApiResponse<List<HymnSummary>> list() {
        List<HymnSummary> items = listHymnsUseCase.listEnabled().stream()
            .map(hymn -> new HymnSummary(hymn.id(), hymn.title(), hymn.number(), hymn.tags()))
            .toList();
        return ApiResponse.success(items);
    }

    @GetMapping("/{id}")
    public ApiResponse<HymnDetailResponse> detail(@PathVariable UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        GetHymnDetailUseCase.HymnDetail detail = getHymnDetailUseCase.getDetail(id, userId);
        Hymn hymn = detail.hymn();
        List<AssetResponse> assets = detail.assets().stream().map(this::toAsset).toList();
        HymnDetailResponse response = new HymnDetailResponse(
            hymn.id(),
            hymn.title(),
            hymn.number(),
            hymn.tags(),
            hymn.enabled(),
            detail.lastOpenedAt(),
            assets
        );
        return ApiResponse.success(response);
    }

    private AssetResponse toAsset(Asset asset) {
        String resolvedUrl = storageService.resolveReadUrl(asset.objectKey(), asset.url());
        return new AssetResponse(
            asset.id(),
            asset.type().name(),
            asset.part() == null ? null : asset.part().name(),
            resolvedUrl,
            asset.checksum(),
            asset.version()
        );
    }

    public record HymnSummary(UUID id, String title, String number, String tags) {
    }

    public record AssetResponse(
        UUID id,
        String type,
        String part,
        String url,
        String checksum,
        String version
    ) {
    }

    public record HymnDetailResponse(
        UUID id,
        String title,
        String number,
        String tags,
        boolean enabled,
        java.time.Instant lastOpenedAt,
        List<AssetResponse> assets
    ) {
    }
}
