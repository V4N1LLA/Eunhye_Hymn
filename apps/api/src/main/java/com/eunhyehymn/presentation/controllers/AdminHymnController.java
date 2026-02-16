package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.AdminCreateHymnUseCase;
import com.eunhyehymn.application.usecases.AdminDeleteHymnUseCase;
import com.eunhyehymn.application.usecases.AdminListHymnsUseCase;
import com.eunhyehymn.application.usecases.AdminUpdateHymnUseCase;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.Hymn;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/hymns")
@Validated
public class AdminHymnController {
    private final AdminCreateHymnUseCase adminCreateHymnUseCase;
    private final AdminDeleteHymnUseCase adminDeleteHymnUseCase;
    private final AdminUpdateHymnUseCase adminUpdateHymnUseCase;
    private final AdminListHymnsUseCase adminListHymnsUseCase;

    public AdminHymnController(
        AdminCreateHymnUseCase adminCreateHymnUseCase,
        AdminDeleteHymnUseCase adminDeleteHymnUseCase,
        AdminUpdateHymnUseCase adminUpdateHymnUseCase,
        AdminListHymnsUseCase adminListHymnsUseCase
    ) {
        this.adminCreateHymnUseCase = adminCreateHymnUseCase;
        this.adminDeleteHymnUseCase = adminDeleteHymnUseCase;
        this.adminUpdateHymnUseCase = adminUpdateHymnUseCase;
        this.adminListHymnsUseCase = adminListHymnsUseCase;
    }

    @PostMapping
    public ApiResponse<HymnResponse> create(@RequestBody @Validated CreateRequest request) {
        Hymn hymn = adminCreateHymnUseCase.create(
            request.title(),
            request.number(),
            request.tags(),
            request.enabled()
        );
        return ApiResponse.success(HymnResponse.from(hymn));
    }

    @PatchMapping("/{id}")
    public ApiResponse<HymnResponse> update(@PathVariable UUID id, @RequestBody UpdateRequest request) {
        Hymn hymn = adminUpdateHymnUseCase.update(
            id,
            request.title(),
            request.number(),
            request.tags(),
            request.enabled()
        );
        return ApiResponse.success(HymnResponse.from(hymn));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        adminDeleteHymnUseCase.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<HymnResponse>> list() {
        List<HymnResponse> items = adminListHymnsUseCase.listAll().stream()
            .map(HymnResponse::from)
            .toList();
        return ApiResponse.success(items);
    }

    public record CreateRequest(
        @NotBlank String title,
        String number,
        String tags,
        Boolean enabled
    ) {
    }

    public record UpdateRequest(
        String title,
        String number,
        String tags,
        Boolean enabled
    ) {
    }

    public record HymnResponse(
        UUID id,
        String title,
        String number,
        String tags,
        boolean enabled
    ) {
        static HymnResponse from(Hymn hymn) {
            return new HymnResponse(hymn.id(), hymn.title(), hymn.number(), hymn.tags(), hymn.enabled());
        }
    }
}
