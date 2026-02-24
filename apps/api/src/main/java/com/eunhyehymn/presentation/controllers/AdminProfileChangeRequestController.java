package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.AdminListProfileChangeRequestsUseCase;
import com.eunhyehymn.application.usecases.AdminReviewProfileChangeRequestUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import com.eunhyehymn.domain.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/profile-change-requests")
@Validated
public class AdminProfileChangeRequestController {
    private final AdminListProfileChangeRequestsUseCase adminListProfileChangeRequestsUseCase;
    private final AdminReviewProfileChangeRequestUseCase adminReviewProfileChangeRequestUseCase;
    private final UserRepository userRepository;

    public AdminProfileChangeRequestController(
        AdminListProfileChangeRequestsUseCase adminListProfileChangeRequestsUseCase,
        AdminReviewProfileChangeRequestUseCase adminReviewProfileChangeRequestUseCase,
        UserRepository userRepository
    ) {
        this.adminListProfileChangeRequestsUseCase = adminListProfileChangeRequestsUseCase;
        this.adminReviewProfileChangeRequestUseCase = adminReviewProfileChangeRequestUseCase;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<ProfileChangeRequestResponse>> list(
        @RequestParam(name = "status", required = false) String status
    ) {
        ProfileChangeRequestStatus normalizedStatus = parseStatus(status);
        List<ProfileChangeRequestResponse> items = adminListProfileChangeRequestsUseCase.list(normalizedStatus).stream()
            .map(this::toResponse)
            .toList();
        return ApiResponse.success(items);
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProfileChangeRequestResponse> review(
        @PathVariable UUID id,
        @RequestBody @Validated ReviewRequest request,
        Authentication authentication
    ) {
        UUID reviewerId = parseUserId(authentication);
        AdminReviewProfileChangeRequestUseCase.Decision decision = parseDecision(request.action());
        ProfileChangeRequest reviewed = adminReviewProfileChangeRequestUseCase.review(
            reviewerId,
            id,
            decision,
            request.rejectReason()
        );
        return ApiResponse.success(toResponse(reviewed));
    }

    private ProfileChangeRequestResponse toResponse(ProfileChangeRequest request) {
        String displayName = userRepository.findById(request.userId())
            .map(user -> user.displayName())
            .orElse(null);
        return new ProfileChangeRequestResponse(
            request.id().toString(),
            request.userId().toString(),
            displayName,
            request.churchName(),
            request.name(),
            request.groupName(),
            request.gender().name(),
            request.status().name(),
            request.requestedAt(),
            request.reviewedBy() == null ? null : request.reviewedBy().toString(),
            request.reviewedAt(),
            request.rejectReason()
        );
    }

    private ProfileChangeRequestStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProfileChangeRequestStatus.PENDING;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return ProfileChangeRequestStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "invalid status: " + raw, null);
        }
    }

    private AdminReviewProfileChangeRequestUseCase.Decision parseDecision(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "action is required", null);
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return AdminReviewProfileChangeRequestUseCase.Decision.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error", "invalid action: " + raw, null);
        }
    }

    private UUID parseUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "authentication is required", null);
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_principal", "invalid authenticated principal", null);
        }
    }

    public record ReviewRequest(
        @NotBlank String action,
        String rejectReason
    ) {
    }

    public record ProfileChangeRequestResponse(
        String id,
        String userId,
        String userDisplayName,
        String churchName,
        String name,
        String group,
        String gender,
        String status,
        java.time.Instant requestedAt,
        String reviewedBy,
        java.time.Instant reviewedAt,
        String rejectReason
    ) {
    }
}
