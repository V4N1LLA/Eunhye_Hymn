package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.application.ports.TokenService;
import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class AdminPasswordLoginUseCase {
    private static final String PROVIDER_LOCAL_ADMIN = "LOCAL_ADMIN";
    private static final String DEFAULT_DISPLAY_NAME = "Admin";

    private final AuthIdentityRepository authIdentityRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final long refreshTokenTtlSeconds;
    private final String configuredLoginId;
    private final String configuredPassword;

    public AdminPasswordLoginUseCase(
        AuthIdentityRepository authIdentityRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        long refreshTokenTtlSeconds,
        String configuredLoginId,
        String configuredPassword
    ) {
        this.authIdentityRepository = authIdentityRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.configuredLoginId = configuredLoginId != null ? configuredLoginId.trim() : "";
        this.configuredPassword = configuredPassword != null ? configuredPassword : "";
    }

    @Transactional
    public LoginResult login(String loginId, String password) {
        if (configuredLoginId.isBlank() || configuredPassword.isBlank()) {
            throw new LoginDisabledException("관리자 ID/PW 로그인이 설정되지 않았습니다");
        }

        String inputLoginId = loginId != null ? loginId.trim() : "";
        String inputPassword = password != null ? password : "";
        if (!secureEquals(configuredLoginId, inputLoginId) || !secureEquals(configuredPassword, inputPassword)) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다");
        }

        Instant now = Instant.now();
        boolean newUser = false;
        User user;

        Optional<AuthIdentity> existingIdentity = authIdentityRepository
            .findByProviderAndProviderSubject(PROVIDER_LOCAL_ADMIN, configuredLoginId);

        if (existingIdentity.isPresent()) {
            UUID userId = existingIdentity.get().userId();
            User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("LOCAL_ADMIN identity user not found: " + userId));

            user = new User(
                existingUser.id(),
                existingUser.displayName() != null ? existingUser.displayName() : DEFAULT_DISPLAY_NAME,
                Role.ADMIN,
                UserStatus.ACTIVE,
                existingUser.createdAt(),
                now
            );
            userRepository.save(user);
        } else {
            newUser = true;
            UUID userId = UUID.randomUUID();
            user = new User(userId, DEFAULT_DISPLAY_NAME, Role.ADMIN, UserStatus.ACTIVE, now, now);
            userRepository.save(user);

            AuthIdentity identity = new AuthIdentity(
                UUID.randomUUID(),
                userId,
                PROVIDER_LOCAL_ADMIN,
                configuredLoginId,
                null,
                now
            );
            authIdentityRepository.save(identity);
        }

        String accessToken = tokenService.issueAccessToken(user.id().toString(), user.role().name());
        String refreshToken = tokenService.issueRefreshToken();
        String hash = tokenHashService.hash(refreshToken);
        RefreshToken stored = new RefreshToken(
            UUID.randomUUID(),
            user.id(),
            hash,
            now.plusSeconds(refreshTokenTtlSeconds),
            null,
            now
        );
        refreshTokenRepository.save(stored);

        return new LoginResult(accessToken, refreshToken, newUser);
    }

    private static boolean secureEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    public record LoginResult(String accessToken, String refreshToken, boolean newUser) {
    }

    public static class LoginDisabledException extends RuntimeException {
        public LoginDisabledException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}

