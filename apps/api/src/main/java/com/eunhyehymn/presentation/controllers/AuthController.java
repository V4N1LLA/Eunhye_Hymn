package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.LogoutUseCase;
import com.eunhyehymn.application.usecases.RefreshTokenUseCase;
import com.eunhyehymn.application.usecases.ValidateInviteCodeUseCase;
import com.eunhyehymn.common.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ValidateInviteCodeUseCase validateInviteCodeUseCase;

    public AuthController(
        RefreshTokenUseCase refreshTokenUseCase,
        LogoutUseCase logoutUseCase,
        ValidateInviteCodeUseCase validateInviteCodeUseCase
    ) {
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.validateInviteCodeUseCase = validateInviteCodeUseCase;
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Validated RefreshRequest request) {
        RefreshTokenUseCase.TokenPair pair = refreshTokenUseCase.refresh(request.refreshToken());
        return ApiResponse.success(new TokenResponse(pair.accessToken(), pair.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Validated LogoutRequest request) {
        logoutUseCase.logout(request.refreshToken());
        return ApiResponse.success(null);
    }

    @PostMapping("/invite/validate")
    public ApiResponse<InviteValidateResponse> validateInvite(@RequestBody @Validated InviteValidateRequest request) {
        boolean valid = validateInviteCodeUseCase.validate(request.code());
        return ApiResponse.success(new InviteValidateResponse(valid));
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken) {
    }

    public record InviteValidateRequest(@NotBlank String code) {
    }

    public record InviteValidateResponse(boolean valid) {
    }
}
