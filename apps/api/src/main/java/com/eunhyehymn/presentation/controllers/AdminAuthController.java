package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.UpdateAdminPasswordCredentialUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@Validated
public class AdminAuthController {
    private final UpdateAdminPasswordCredentialUseCase updateAdminPasswordCredentialUseCase;

    public AdminAuthController(UpdateAdminPasswordCredentialUseCase updateAdminPasswordCredentialUseCase) {
        this.updateAdminPasswordCredentialUseCase = updateAdminPasswordCredentialUseCase;
    }

    @PostMapping("/password")
    public ApiResponse<UpdateAdminCredentialResponse> updateCredential(
        @RequestBody @Validated UpdateAdminCredentialRequest request,
        Authentication authentication
    ) {
        UUID requesterId = parseRequesterId(authentication);
        try {
            UpdateAdminPasswordCredentialUseCase.Result result = updateAdminPasswordCredentialUseCase.update(
                requesterId,
                request.currentPassword(),
                request.newLoginId(),
                request.newPassword()
            );
            return ApiResponse.success(new UpdateAdminCredentialResponse(result.loginId(), result.updatedAt()));
        } catch (UpdateAdminPasswordCredentialUseCase.LoginDisabledException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "admin_login_disabled", e.getMessage(), null);
        } catch (UpdateAdminPasswordCredentialUseCase.InvalidCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "admin_password_change_failed", e.getMessage(), null);
        } catch (UpdateAdminPasswordCredentialUseCase.InvalidCredentialFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", e.getMessage(), null);
        }
    }

    private UUID parseRequesterId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_principal", "Authentication principal is missing.", null);
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_principal", "Authentication principal is not a valid UUID.", null);
        }
    }

    public record UpdateAdminCredentialRequest(
        @NotBlank String currentPassword,
        @NotBlank String newLoginId,
        @NotBlank String newPassword
    ) {
    }

    public record UpdateAdminCredentialResponse(String loginId, Instant updatedAt) {
    }
}
