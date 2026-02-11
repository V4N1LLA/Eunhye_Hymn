package com.eunhyehymn.common.config;

import com.eunhyehymn.application.ports.SocialTokenVerifier;
import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.application.ports.TokenService;
import com.eunhyehymn.application.usecases.DevLoginUseCase;
import com.eunhyehymn.application.usecases.LogoutUseCase;
import com.eunhyehymn.application.usecases.RefreshTokenUseCase;
import com.eunhyehymn.application.usecases.SocialLoginUseCase;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.infrastructure.security.JwtAuthenticationFilter;
import com.eunhyehymn.infrastructure.security.JwtService;
import com.eunhyehymn.infrastructure.security.JwtTokenService;
import com.eunhyehymn.infrastructure.security.Sha256TokenHashService;
import com.eunhyehymn.infrastructure.security.SocialTokenVerifierImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfig {
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
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    SocialTokenVerifier socialTokenVerifier(
        ObjectMapper objectMapper,
        @Value("${security.social.google.client-id:}") String googleClientId
    ) {
        return new SocialTokenVerifierImpl(objectMapper, googleClientId);
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
        InviteCodeRepository inviteCodeRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        @Value("${security.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds
    ) {
        return new SocialLoginUseCase(
            socialTokenVerifier,
            authIdentityRepository,
            userRepository,
            inviteCodeRepository,
            refreshTokenRepository,
            tokenService,
            tokenHashService,
            refreshTokenTtlSeconds
        );
    }
}
