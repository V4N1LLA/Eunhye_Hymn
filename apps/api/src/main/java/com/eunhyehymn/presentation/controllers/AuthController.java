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
import com.eunhyehymn.infrastructure.security.AuthRateLimitService;
import com.eunhyehymn.infrastructure.security.SocialLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
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
    private final AuthRateLimitService authRateLimitService;
    private final boolean trustForwardedIpHeader;

    public AuthController(
        AdminPasswordLoginUseCase adminPasswordLoginUseCase,
        RefreshTokenUseCase refreshTokenUseCase,
        LogoutUseCase logoutUseCase,
        ValidateInviteCodeUseCase validateInviteCodeUseCase,
        SocialLoginUseCase socialLoginUseCase,
        UserPasswordSignupUseCase userPasswordSignupUseCase,
        UserPasswordLoginUseCase userPasswordLoginUseCase,
        AuthRateLimitService authRateLimitService,
        @Value("${security.rate-limit.auth.trust-forwarded-ip-header:false}") boolean trustForwardedIpHeader
    ) {
        this.adminPasswordLoginUseCase = adminPasswordLoginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.validateInviteCodeUseCase = validateInviteCodeUseCase;
        this.socialLoginUseCase = socialLoginUseCase;
        this.userPasswordSignupUseCase = userPasswordSignupUseCase;
        this.userPasswordLoginUseCase = userPasswordLoginUseCase;
        this.authRateLimitService = authRateLimitService;
        this.trustForwardedIpHeader = trustForwardedIpHeader;
    }

    @PostMapping("/admin/login")
    public ApiResponse<SocialLoginResponse> adminPasswordLogin(
        @RequestBody @Validated AdminPasswordLoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String key = keyByIpAndLoginId(httpRequest, request.loginId());
        authRateLimitService.checkOrThrow("admin_login", key);

        try {
            AdminPasswordLoginUseCase.LoginResult result = adminPasswordLoginUseCase.login(
                request.loginId(),
                request.password()
            );
            authRateLimitService.recordOutcome("admin_login", true);
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(), result.refreshToken(), result.newUser()
            ));
        } catch (AdminPasswordLoginUseCase.LoginDisabledException e) {
            authRateLimitService.recordOutcome("admin_login", false);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "admin_login_disabled", e.getMessage(), null);
        } catch (AdminPasswordLoginUseCase.InvalidCredentialsException e) {
            authRateLimitService.recordOutcome("admin_login", false);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "admin_login_failed", e.getMessage(), null);
        }
    }

    @PostMapping("/social")
    public ApiResponse<SocialLoginResponse> socialLogin(
        @RequestBody @Validated SocialLoginRequest request,
        HttpServletRequest httpRequest
    ) {
        authRateLimitService.checkOrThrow("social_login", keyByIp(httpRequest));
        try {
            SocialLoginUseCase.LoginResult result = socialLoginUseCase.login(
                request.provider(), request.token(), request.inviteCode()
            );
            authRateLimitService.recordOutcome("social_login", true);
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(), result.refreshToken(), result.newUser()
            ));
        } catch (SocialLoginException e) {
            authRateLimitService.recordOutcome("social_login", false);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "social_auth_failed", e.getMessage(), null);
        } catch (SocialLoginUseCase.AccountDisabledException e) {
            authRateLimitService.recordOutcome("social_login", false);
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", e.getMessage(), null);
        } catch (SocialLoginUseCase.AdminOnlyException e) {
            authRateLimitService.recordOutcome("social_login", false);
            throw new ApiException(HttpStatus.FORBIDDEN, "admin_only", e.getMessage(), null);
        } catch (SocialLoginUseCase.InvalidInviteCodeException e) {
            authRateLimitService.recordOutcome("social_login", false);
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_invite_code", e.getMessage(), null);
        }
    }

    @PostMapping("/signup")
    public ApiResponse<SocialLoginResponse> signup(
        @RequestBody @Validated UserPasswordSignupRequest request,
        HttpServletRequest httpRequest
    ) {
        String key = keyByIpAndLoginId(httpRequest, request.loginId());
        authRateLimitService.checkOrThrow("user_signup", key);

        try {
            UserPasswordSignupUseCase.LoginResult result = userPasswordSignupUseCase.signup(
                request.loginId(),
                request.password(),
                request.inviteCode()
            );
            authRateLimitService.recordOutcome("user_signup", true);
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(),
                result.refreshToken(),
                result.newUser()
            ));
        } catch (UserPasswordSignupUseCase.InvalidInviteCodeException e) {
            authRateLimitService.recordOutcome("user_signup", false);
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_invite_code", e.getMessage(), null);
        } catch (UserPasswordSignupUseCase.DuplicateLoginIdException e) {
            authRateLimitService.recordOutcome("user_signup", false);
            throw new ApiException(HttpStatus.CONFLICT, "login_id_exists", e.getMessage(), null);
        } catch (UserPasswordSignupUseCase.InvalidCredentialFormatException e) {
            authRateLimitService.recordOutcome("user_signup", false);
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_credential_format", e.getMessage(), null);
        }
    }

    @PostMapping("/login")
    public ApiResponse<SocialLoginResponse> login(
        @RequestBody @Validated UserPasswordLoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String key = keyByIpAndLoginId(httpRequest, request.loginId());
        authRateLimitService.checkOrThrow("user_login", key);

        try {
            UserPasswordLoginUseCase.LoginResult result = userPasswordLoginUseCase.login(
                request.loginId(),
                request.password()
            );
            authRateLimitService.recordOutcome("user_login", true);
            return ApiResponse.success(new SocialLoginResponse(
                result.accessToken(),
                result.refreshToken(),
                result.newUser()
            ));
        } catch (UserPasswordLoginUseCase.InvalidCredentialsException e) {
            authRateLimitService.recordOutcome("user_login", false);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "user_login_failed", e.getMessage(), null);
        } catch (UserPasswordLoginUseCase.AccountDisabledException e) {
            authRateLimitService.recordOutcome("user_login", false);
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", e.getMessage(), null);
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
    public ApiResponse<InviteValidateResponse> validateInvite(
        @RequestBody @Validated InviteValidateRequest request,
        HttpServletRequest httpRequest
    ) {
        authRateLimitService.checkOrThrow("invite_validate", keyByIp(httpRequest));
        boolean valid = validateInviteCodeUseCase.validate(request.code());
        authRateLimitService.recordOutcome("invite_validate", valid);
        return ApiResponse.success(new InviteValidateResponse(valid));
    }

    private String keyByIp(HttpServletRequest request) {
        return resolveClientIp(request);
    }

    private String keyByIpAndLoginId(HttpServletRequest request, String loginId) {
        String normalizedLoginId = loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
        return resolveClientIp(request) + "|" + normalizedLoginId;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (!trustForwardedIpHeader) {
            return normalizeIp(request.getRemoteAddr());
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String candidate = normalizeIp(forwardedFor.split(",")[0]);
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            String candidate = normalizeIp(realIp);
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private String normalizeIp(String source) {
        if (source == null) {
            return "";
        }
        return source.trim();
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
