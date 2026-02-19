package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.AdminPasswordLoginUseCase;
import com.eunhyehymn.application.usecases.LogoutUseCase;
import com.eunhyehymn.application.usecases.RefreshTokenUseCase;
import com.eunhyehymn.application.usecases.SocialLoginUseCase;
import com.eunhyehymn.application.usecases.UserPasswordLoginUseCase;
import com.eunhyehymn.application.usecases.UserPasswordSignupUseCase;
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
    private final AdminPasswordLoginUseCase adminPasswordLoginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ValidateInviteCodeUseCase validateInviteCodeUseCase;
    private final SocialLoginUseCase socialLoginUseCase;
    private final UserPasswordSignupUseCase userPasswordSignupUseCase;
    private final UserPasswordLoginUseCase userPasswordLoginUseCase;

    public AuthController(
        AdminPasswordLoginUseCase adminPasswordLoginUseCase,
        RefreshTokenUseCase refreshTokenUseCase,
        LogoutUseCase logoutUseCase,
        ValidateInviteCodeUseCase validateInviteCodeUseCase,
        SocialLoginUseCase socialLoginUseCase,
        UserPasswordSignupUseCase userPasswordSignupUseCase,
        UserPasswordLoginUseCase userPasswordLoginUseCase
    ) {
        this.adminPasswordLoginUseCase = adminPasswordLoginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.validateInviteCodeUseCase = validateInviteCodeUseCase;
        this.socialLoginUseCase = socialLoginUseCase;
        this.userPasswordSignupUseCase = userPasswordSignupUseCase;
        this.userPasswordLoginUseCase = userPasswordLoginUseCase;
    }

    @PostMapping("/admin/login")
    public ApiResponse<SocialLoginResponse> adminPasswordLogin(
        @RequestBody @Validated AdminPasswordLoginRequest request
    ) {
        try {
            AdminPasswordLoginUseCase.LoginResult result = adminPasswordLoginUseCase.login(
                request.loginId(),
                request.password()
            );
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(), result.refreshToken(), result.newUser()
            ));
        } catch (AdminPasswordLoginUseCase.LoginDisabledException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "admin_login_disabled", e.getMessage(), null);
        } catch (AdminPasswordLoginUseCase.InvalidCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "admin_login_failed", e.getMessage(), null);
        }
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
        } catch (SocialLoginUseCase.AdminOnlyException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "admin_only", e.getMessage(), null);
        } catch (SocialLoginUseCase.InvalidInviteCodeException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_invite_code", e.getMessage(), null);
        }
    }

    @PostMapping("/signup")
    public ApiResponse<SocialLoginResponse> signup(@RequestBody @Validated UserPasswordSignupRequest request) {
        try {
            UserPasswordSignupUseCase.LoginResult result = userPasswordSignupUseCase.signup(
                request.loginId(),
                request.password(),
                request.inviteCode()
            );
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(),
                result.refreshToken(),
                result.newUser()
            ));
        } catch (UserPasswordSignupUseCase.InvalidInviteCodeException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_invite_code", e.getMessage(), null);
        } catch (UserPasswordSignupUseCase.DuplicateLoginIdException e) {
            throw new ApiException(HttpStatus.CONFLICT, "login_id_exists", e.getMessage(), null);
        } catch (UserPasswordSignupUseCase.InvalidCredentialFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_credential_format", e.getMessage(), null);
        }
    }

    @PostMapping("/login")
    public ApiResponse<SocialLoginResponse> login(@RequestBody @Validated UserPasswordLoginRequest request) {
        try {
            UserPasswordLoginUseCase.LoginResult result = userPasswordLoginUseCase.login(
                request.loginId(),
                request.password()
            );
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(),
                result.refreshToken(),
                result.newUser()
            ));
        } catch (UserPasswordLoginUseCase.InvalidCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "user_login_failed", e.getMessage(), null);
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

    public record AdminPasswordLoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
    ) {
    }

    public record UserPasswordSignupRequest(
        @NotBlank String loginId,
        @NotBlank String password,
        @NotBlank String inviteCode
    ) {
    }

    public record UserPasswordLoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
    ) {
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
