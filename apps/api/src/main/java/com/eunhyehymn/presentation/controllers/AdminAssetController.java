package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.ports.StorageService;
import com.eunhyehymn.application.usecases.AdminConfirmAssetUseCase;
import com.eunhyehymn.application.usecases.AdminDeleteAssetUseCase;
import com.eunhyehymn.application.usecases.AdminPresignAssetUseCase;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.Asset;
import com.eunhyehymn.domain.model.AssetType;
import com.eunhyehymn.domain.model.PartType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/assets")
@Validated
public class AdminAssetController {
    private final AdminPresignAssetUseCase adminPresignAssetUseCase;
    private final AdminConfirmAssetUseCase adminConfirmAssetUseCase;
    private final AdminDeleteAssetUseCase adminDeleteAssetUseCase;

    public AdminAssetController(
        AdminPresignAssetUseCase adminPresignAssetUseCase,
        AdminConfirmAssetUseCase adminConfirmAssetUseCase,
        AdminDeleteAssetUseCase adminDeleteAssetUseCase
    ) {
        this.adminPresignAssetUseCase = adminPresignAssetUseCase;
        this.adminConfirmAssetUseCase = adminConfirmAssetUseCase;
        this.adminDeleteAssetUseCase = adminDeleteAssetUseCase;
    }

    @PostMapping("/presign")
    public ApiResponse<PresignResponse> presign(@RequestBody @Validated PresignRequest request) {
        StorageService.PresignResult result = adminPresignAssetUseCase.presign(
            request.hymnId(),
            request.type(),
            request.part(),
            request.filename(),
            request.contentType()
        );
        return ApiResponse.success(new PresignResponse(result.uploadUrl(), result.publicUrl(), result.objectKey()));
    }

    @PostMapping("/confirm")
    public ApiResponse<ConfirmResponse> confirm(@RequestBody @Validated ConfirmRequest request) {
        Asset asset = adminConfirmAssetUseCase.confirm(
            request.hymnId(),
            request.type(),
            request.part(),
            request.publicUrl(),
            request.objectKey(),
            request.checksum(),
            request.version()
        );
        return ApiResponse.success(new ConfirmResponse(
            asset.id(),
            asset.hymnId(),
            asset.type(),
            asset.part(),
            asset.url(),
            asset.objectKey()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAsset(@PathVariable UUID id) {
        adminDeleteAssetUseCase.delete(id);
        return ApiResponse.success(null);
    }

    public record PresignRequest(
        @NotNull UUID hymnId,
        @NotNull AssetType type,
        PartType part,
        @NotBlank String filename,
        @NotBlank String contentType
    ) {
    }

    public record PresignResponse(String uploadUrl, String publicUrl, String objectKey) {
    }

    public record ConfirmRequest(
        @NotNull UUID hymnId,
        @NotNull AssetType type,
        PartType part,
        @NotBlank String publicUrl,
        @NotBlank String objectKey,
        String checksum,
        String version
    ) {
    }

    public record ConfirmResponse(
        UUID assetId,
        UUID hymnId,
        AssetType type,
        PartType part,
        String url,
        String objectKey
    ) {
    }
}
