package com.eunhyehymn.common.config;

import com.eunhyehymn.application.ports.SmsSender;
import com.eunhyehymn.application.ports.SocialTokenVerifier;
import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.application.ports.TokenService;
import com.eunhyehymn.application.usecases.AdminPasswordLoginUseCase;
import com.eunhyehymn.application.usecases.ConfirmInviteCodeUseCase;
import com.eunhyehymn.application.usecases.DevLoginUseCase;
import com.eunhyehymn.application.usecases.LogoutUseCase;
import com.eunhyehymn.application.usecases.RefreshTokenUseCase;
import com.eunhyehymn.application.usecases.RequestSmsCodeUseCase;
import com.eunhyehymn.application.usecases.SocialLoginUseCase;
import com.eunhyehymn.application.usecases.UpdateAdminPasswordCredentialUseCase;
import com.eunhyehymn.application.usecases.UserPasswordLoginUseCase;
import com.eunhyehymn.application.usecases.UserPasswordSignupUseCase;
import com.eunhyehymn.application.usecases.ValidateInviteCodeUseCase;
import com.eunhyehymn.application.usecases.VerifySmsCodeUseCase;
import com.eunhyehymn.application.usecases.WithdrawAccountUseCase;
import com.eunhyehymn.domain.repository.AdminPasswordCredentialRepository;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.SmsVerificationRequestRepository;
import com.eunhyehymn.domain.repository.UserPasswordCredentialRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import com.eunhyehymn.infrastructure.security.JwtAuthenticationFilter;
import com.eunhyehymn.infrastructure.security.JwtService;
import com.eunhyehymn.infrastructure.security.JwtTokenService;
import com.eunhyehymn.infrastructure.security.LogOnlySmsSender;
import com.eunhyehymn.infrastructure.security.Sha256TokenHashService;
import com.eunhyehymn.infrastructure.security.SocialTokenVerifierImpl;
import com.eunhyehymn.infrastructure.security.TwilioSmsSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfig {
    private static Set<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }

    @Bean
    JwtService jwtService(
        ObjectMapper objectMapper,
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds
    ) {
        return new JwtService(objectMapper, secret, accessTokenTtlSeconds);
    }

    @Bean
    TokenService tokenService(JwtService jwtService) {
        return new JwtTokenService(jwtService);
    }

    @Bean
    TokenHashService tokenHashService() {
        return new Sha256TokenHashService();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        return new JwtAuthenticationFilter(jwtService, userRepository);
    }

    @Bean
    SocialTokenVerifier socialTokenVerifier(ObjectMapper objectMapper) {
        return new SocialTokenVerifierImpl(objectMapper);
    }

    @Bean
    SmsSender smsSender(
        @Value("${sms.twilio.enabled:false}") boolean twilioEnabled,
        @Value("${sms.twilio.account-sid:}") String accountSid,
        @Value("${sms.twilio.auth-token:}") String authToken,
        @Value("${sms.twilio.from-number:}") String fromNumber,
        @Value("${sms.twilio.message-template:[Eunhye Hymn] Verification code is %s.}") String template
    ) {
        if (!twilioEnabled) {
            return new LogOnlySmsSender();
        }
        if (accountSid == null || accountSid.isBlank() ||
            authToken == null || authToken.isBlank() ||
            fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio SMS is enabled but credentials are missing.");
        }
        return new TwilioSmsSender(accountSid.trim(), authToken.trim(), fromNumber.trim(), template);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        return new RefreshTokenUseCase(
            refreshTokenRepository,
            userRepository,
            tokenService,
            tokenHashService,
            refreshTokenTtlSeconds
        );
    }

    @Bean
    LogoutUseCase logoutUseCase(
        RefreshTokenRepository refreshTokenRepository,
        TokenHashService tokenHashService
    ) {
        return new LogoutUseCase(refreshTokenRepository, tokenHashService);
    }

    @Bean
    DevLoginUseCase devLoginUseCase(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        return new DevLoginUseCase(
            userRepository,
            refreshTokenRepository,
            tokenService,
            tokenHashService,
            refreshTokenTtlSeconds
        );
    }

    @Bean
    SocialLoginUseCase socialLoginUseCase(
        SocialTokenVerifier socialTokenVerifier,
        AuthIdentityRepository authIdentityRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds,
        @Value("${security.admin.emails:}") String adminEmails,
        @Value("${security.admin.kakao-subjects:}") String adminKakaoSubjects,
        @Value("${security.admin.enforce-admin-only:false}") boolean enforceAdminOnly
    ) {
        return new SocialLoginUseCase(
            socialTokenVerifier,
            authIdentityRepository,
            userRepository,
            refreshTokenRepository,
            tokenService,
            tokenHashService,
            refreshTokenTtlSeconds,
            splitCsv(adminEmails).stream().map(s -> s.toLowerCase()).collect(Collectors.toUnmodifiableSet()),
            splitCsv(adminKakaoSubjects),
            enforceAdminOnly
        );
    }

    @Bean
    AdminPasswordLoginUseCase adminPasswordLoginUseCase(
        AdminPasswordCredentialRepository adminPasswordCredentialRepository,
        AuthIdentityRepository authIdentityRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        PasswordEncoder passwordEncoder,
        @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds,
        @Value("${security.admin.login-id:}") String loginId,
        @Value("${security.admin.login-password:}") String loginPassword
    ) {
        return new AdminPasswordLoginUseCase(
            adminPasswordCredentialRepository,
            authIdentityRepository,
            userRepository,
            refreshTokenRepository,
            tokenService,
            tokenHashService,
            passwordEncoder,
            refreshTokenTtlSeconds,
            loginId,
            loginPassword
        );
    }

    @Bean
    UserPasswordSignupUseCase userPasswordSignupUseCase(
        AuthIdentityRepository authIdentityRepository,
        UserPasswordCredentialRepository userPasswordCredentialRepository,
        UserRepository userRepository,
        InviteCodeRepository inviteCodeRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        PasswordEncoder passwordEncoder,
        @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        return new UserPasswordSignupUseCase(
            authIdentityRepository,
            userPasswordCredentialRepository,
            userRepository,
            inviteCodeRepository,
            refreshTokenRepository,
            tokenService,
            tokenHashService,
            passwordEncoder,
            refreshTokenTtlSeconds
        );
    }

    @Bean
    UserPasswordLoginUseCase userPasswordLoginUseCase(
        AuthIdentityRepository authIdentityRepository,
        UserPasswordCredentialRepository userPasswordCredentialRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        PasswordEncoder passwordEncoder,
        @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        return new UserPasswordLoginUseCase(
            authIdentityRepository,
            userPasswordCredentialRepository,
            userRepository,
            refreshTokenRepository,
            tokenService,
            tokenHashService,
            passwordEncoder,
            refreshTokenTtlSeconds
        );
    }

    @Bean
    ConfirmInviteCodeUseCase confirmInviteCodeUseCase(
        ValidateInviteCodeUseCase validateInviteCodeUseCase,
        InviteCodeRepository inviteCodeRepository,
        UserRepository userRepository,
        UserVerificationRepository userVerificationRepository
    ) {
        return new ConfirmInviteCodeUseCase(
            validateInviteCodeUseCase,
            inviteCodeRepository,
            userRepository,
            userVerificationRepository
        );
    }

    @Bean
    RequestSmsCodeUseCase requestSmsCodeUseCase(
        SmsVerificationRequestRepository smsVerificationRequestRepository,
        UserRepository userRepository,
        UserVerificationRepository userVerificationRepository,
        TokenHashService tokenHashService,
        SmsSender smsSender,
        @Value("${sms.verification.code-length:6}") int codeLength,
        @Value("${sms.verification.expires-seconds:300}") long expiresSeconds,
        @Value("${sms.verification.cooldown-seconds:30}") long cooldownSeconds
    ) {
        return new RequestSmsCodeUseCase(
            smsVerificationRequestRepository,
            userRepository,
            userVerificationRepository,
            tokenHashService,
            smsSender,
            codeLength,
            expiresSeconds,
            cooldownSeconds
        );
    }

    @Bean
    VerifySmsCodeUseCase verifySmsCodeUseCase(
        SmsVerificationRequestRepository smsVerificationRequestRepository,
        UserRepository userRepository,
        UserVerificationRepository userVerificationRepository,
        TokenHashService tokenHashService,
        @Value("${sms.verification.code-length:6}") int codeLength,
        @Value("${sms.verification.max-attempts:5}") int maxAttempts
    ) {
        return new VerifySmsCodeUseCase(
            smsVerificationRequestRepository,
            userRepository,
            userVerificationRepository,
            tokenHashService,
            codeLength,
            maxAttempts
        );
    }

    @Bean
    WithdrawAccountUseCase withdrawAccountUseCase(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        UserVerificationRepository userVerificationRepository,
        SmsVerificationRequestRepository smsVerificationRequestRepository
    ) {
        return new WithdrawAccountUseCase(
            userRepository,
            refreshTokenRepository,
            userVerificationRepository,
            smsVerificationRequestRepository
        );
    }

    @Bean
    UpdateAdminPasswordCredentialUseCase updateAdminPasswordCredentialUseCase(
        AdminPasswordCredentialRepository adminPasswordCredentialRepository,
        AuthIdentityRepository authIdentityRepository,
        PasswordEncoder passwordEncoder,
        @Value("${security.admin.login-id:}") String loginId,
        @Value("${security.admin.login-password:}") String loginPassword
    ) {
        return new UpdateAdminPasswordCredentialUseCase(
            adminPasswordCredentialRepository,
            authIdentityRepository,
            passwordEncoder,
            loginId,
            loginPassword
        );
    }
}
