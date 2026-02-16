package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.AdminCreateInviteCodeUseCase;
import com.eunhyehymn.application.usecases.AdminListInviteCodesUseCase;
import com.eunhyehymn.application.usecases.AdminRevokeInviteCodeUseCase;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.InviteCode;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/invite-codes")
@Validated
public class AdminInviteCodeController {
    private final AdminCreateInviteCodeUseCase createUseCase;
    private final AdminListInviteCodesUseCase listUseCase;
    private final AdminRevokeInviteCodeUseCase revokeUseCase;

    public AdminInviteCodeController(
        AdminCreateInviteCodeUseCase createUseCase,
        AdminListInviteCodesUseCase listUseCase,
        AdminRevokeInviteCodeUseCase revokeUseCase
    ) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.revokeUseCase = revokeUseCase;
    }

    @PostMapping
    public ApiResponse<InviteCodeResponse> create(
        @RequestBody @Validated CreateInviteCodeRequest request,
        Authentication authentication
    ) {
        UUID createdBy = UUID.fromString(authentication.getName());
        InviteCode created = createUseCase.create(
            request.code(), createdBy, request.description(), request.maxUses(), request.expiresAt()
        );
        return ApiResponse.success(InviteCodeResponse.from(created));
    }

    @GetMapping
    public ApiResponse<List<InviteCodeResponse>> list() {
        List<InviteCodeResponse> items = listUseCase.listAll().stream()
            .map(InviteCodeResponse::from)
            .toList();
        return ApiResponse.success(items);
    }

    @DeleteMapping("/{code}")
    public ApiResponse<Void> revoke(@PathVariable String code) {
        revokeUseCase.revoke(code);
        return ApiResponse.success(null);
    }

    public record CreateInviteCodeRequest(
        @NotBlank String code,
        String description,
        Integer maxUses,
        Instant expiresAt
    ) {
    }

    public record InviteCodeResponse(
        String code,
        UUID createdBy,
        String description,
        Integer maxUses,
        int usedCount,
        boolean enabled,
        Instant expiresAt,
        Instant createdAt
    ) {
        static InviteCodeResponse from(InviteCode ic) {
            return new InviteCodeResponse(
                ic.code(), ic.createdBy(), ic.description(),
                ic.maxUses(), ic.usedCount(), ic.enabled(),
                ic.expiresAt(), ic.createdAt()
            );
        }
    }
}
