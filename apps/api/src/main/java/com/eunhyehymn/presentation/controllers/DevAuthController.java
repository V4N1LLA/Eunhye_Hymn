package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.DevLoginUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/dev")
@Profile("dev")
@Validated
public class DevAuthController {
    private final DevLoginUseCase devLoginUseCase;

    public DevAuthController(DevLoginUseCase devLoginUseCase) {
        this.devLoginUseCase = devLoginUseCase;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Validated DevLoginRequest request) {
        Role role;
        try {
            role = Role.valueOf(request.role());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_role", "ROLE 값이 유효하지 않습니다", null);
        }

        DevLoginUseCase.TokenPair pair = devLoginUseCase.login(request.userId(), role, request.displayName());
        return ApiResponse.success(new TokenResponse(pair.accessToken(), pair.refreshToken()));
    }

    public record DevLoginRequest(
        @NotNull UUID userId,
        @NotBlank String role,
        @NotBlank String displayName
    ) {
    }

    public record TokenResponse(String accessToken, String refreshToken) {
    }
}
