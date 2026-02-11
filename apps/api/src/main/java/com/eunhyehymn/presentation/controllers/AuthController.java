package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.LogoutUseCase;
import com.eunhyehymn.application.usecases.RefreshTokenUseCase;
import com.eunhyehymn.application.usecases.SocialLoginUseCase;
import com.eunhyehymn.application.usecases.ValidateInviteCodeUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.infrastructure.security.SocialLoginException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
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
    private final SocialLoginUseCase socialLoginUseCase;

    public AuthController(
        RefreshTokenUseCase refreshTokenUseCase,
        LogoutUseCase logoutUseCase,
        ValidateInviteCodeUseCase validateInviteCodeUseCase,
        SocialLoginUseCase socialLoginUseCase
    ) {
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.validateInviteCodeUseCase = validateInviteCodeUseCase;
        this.socialLoginUseCase = socialLoginUseCase;
    }

    @PostMapping("/social")
    public ApiResponse<SocialLoginResponse> socialLogin(@RequestBody @Validated SocialLoginRequest request) {
        try {
            SocialLoginUseCase.LoginResult result = socialLoginUseCase.login(
                request.provider(), request.token(), request.inviteCode()
            );
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(), result.refreshToken(), result.newUser()
            ));
        } catch (SocialLoginException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "social_auth_failed", e.getMessage(), null);
        } catch (SocialLoginUseCase.InvalidInviteCodeException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_invite_code", e.getMessage(), null);
        }
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

    public record SocialLoginRequest(
        @NotBlank String provider,
        @NotBlank String token,
        String inviteCode
    ) {
    }

    public record SocialLoginResponse(String accessToken, String refreshToken, boolean newUser) {
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
