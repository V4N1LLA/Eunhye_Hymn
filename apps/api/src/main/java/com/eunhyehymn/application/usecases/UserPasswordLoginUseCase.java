package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.application.ports.TokenService;
import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserPasswordCredential;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.UserPasswordCredentialRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class UserPasswordLoginUseCase {
    private final AuthIdentityRepository authIdentityRepository;
    private final UserPasswordCredentialRepository userPasswordCredentialRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenTtlSeconds;

    public UserPasswordLoginUseCase(
        AuthIdentityRepository authIdentityRepository,
        UserPasswordCredentialRepository userPasswordCredentialRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        PasswordEncoder passwordEncoder,
        long refreshTokenTtlSeconds
    ) {
        this.authIdentityRepository = authIdentityRepository;
        this.userPasswordCredentialRepository = userPasswordCredentialRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public LoginResult login(String loginId, String password) {
        String normalizedLoginId = UserPasswordSignupUseCase.normalizeLookupLoginId(loginId);
        String normalizedPassword = password == null ? "" : password;

        AuthIdentity identity = authIdentityRepository
            .findByProviderAndProviderSubject(UserPasswordSignupUseCase.PROVIDER_LOCAL_USER, normalizedLoginId)
            .orElseThrow(() -> new InvalidCredentialsException("로그인 ID 또는 비밀번호가 올바르지 않습니다."));

        UserPasswordCredential credential = userPasswordCredentialRepository.findByUserId(identity.userId())
            .orElseThrow(() -> new InvalidCredentialsException("로그인 ID 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(normalizedPassword, credential.passwordHash())) {
            throw new InvalidCredentialsException("로그인 ID 또는 비밀번호가 올바르지 않습니다.");
        }

        Instant now = Instant.now();
        User existing = userRepository.findById(identity.userId())
            .orElseThrow(() -> new IllegalStateException("LOCAL_USER identity user not found: " + identity.userId()));
        User user = new User(
            existing.id(),
            existing.displayName(),
            existing.role(),
            existing.status(),
            existing.createdAt(),
            now
        );
        userRepository.save(user);

        String accessToken = tokenService.issueAccessToken(user.id().toString(), user.role().name());
        String refreshToken = tokenService.issueRefreshToken();
        String hash = tokenHashService.hash(refreshToken);
        refreshTokenRepository.save(new RefreshToken(
            UUID.randomUUID(),
            user.id(),
            hash,
            now.plusSeconds(refreshTokenTtlSeconds),
            null,
            now
        ));

        return new LoginResult(accessToken, refreshToken, false);
    }

    public record LoginResult(String accessToken, String refreshToken, boolean newUser) {
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}
