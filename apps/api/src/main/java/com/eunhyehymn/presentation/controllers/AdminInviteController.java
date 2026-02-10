package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.CreateInviteUseCase;
import com.eunhyehymn.application.usecases.ListInvitesUseCase;
import com.eunhyehymn.application.usecases.RevokeInviteUseCase;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.InviteCode;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/invites")
@Validated
public class AdminInviteController {
    private final CreateInviteUseCase createInviteUseCase;
    private final RevokeInviteUseCase revokeInviteUseCase;
    private final ListInvitesUseCase listInvitesUseCase;

    public AdminInviteController(
        CreateInviteUseCase createInviteUseCase,
        RevokeInviteUseCase revokeInviteUseCase,
        ListInvitesUseCase listInvitesUseCase
    ) {
        this.createInviteUseCase = createInviteUseCase;
        this.revokeInviteUseCase = revokeInviteUseCase;
        this.listInvitesUseCase = listInvitesUseCase;
    }

    @PostMapping
    public ApiResponse<InviteResponse> create(@RequestBody @Validated CreateRequest request) {
        InviteCode created = createInviteUseCase.create(
            request.code(), request.maxUses(), request.expiresAt()
        );
        return ApiResponse.success(InviteResponse.from(created));
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<InviteResponse> revoke(@PathVariable UUID id) {
        InviteCode revoked = revokeInviteUseCase.revoke(id);
        return ApiResponse.success(InviteResponse.from(revoked));
    }

    @GetMapping
    public ApiResponse<List<InviteResponse>> list() {
        List<InviteResponse> items = listInvitesUseCase.listAll().stream()
            .map(InviteResponse::from)
            .toList();
        return ApiResponse.success(items);
    }

    public record CreateRequest(
        @NotBlank String code,
        Integer maxUses,
        Instant expiresAt
    ) {
    }

    public record InviteResponse(
        UUID id,
        String code,
        Integer maxUses,
        int usedCount,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
    ) {
        static InviteResponse from(InviteCode ic) {
            return new InviteResponse(
                ic.id(), ic.code(), ic.maxUses(), ic.usedCount(),
                ic.expiresAt(), ic.revokedAt(), ic.createdAt()
            );
        }
    }
}
