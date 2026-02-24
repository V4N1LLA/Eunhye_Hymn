package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.application.ports.TokenService;
import com.eunhyehymn.domain.model.AdminPasswordCredential;
import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.AdminPasswordCredentialRepository;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class AdminPasswordLoginUseCase {
    static final String PROVIDER_LOCAL_ADMIN = "LOCAL_ADMIN";
    private static final String DEFAULT_DISPLAY_NAME = "Admin";

    private final AdminPasswordCredentialRepository adminPasswordCredentialRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenTtlSeconds;
    private final String configuredLoginId;
    private final String configuredPassword;

    public AdminPasswordLoginUseCase(
        AdminPasswordCredentialRepository adminPasswordCredentialRepository,
        AuthIdentityRepository authIdentityRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        PasswordEncoder passwordEncoder,
        long refreshTokenTtlSeconds,
        String configuredLoginId,
        String configuredPassword
    ) {
        this.adminPasswordCredentialRepository = adminPasswordCredentialRepository;
        this.authIdentityRepository = authIdentityRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.configuredLoginId = configuredLoginId != null ? configuredLoginId.trim() : "";
        this.configuredPassword = configuredPassword != null ? configuredPassword : "";
    }

    @Transactional
    public LoginResult login(String loginId, String password) {
        EffectiveCredential credential = resolveEffectiveCredential();

        String inputLoginId = loginId != null ? loginId.trim() : "";
        String inputPassword = password != null ? password : "";
        if (!secureEquals(credential.loginId(), inputLoginId) || !verifyPassword(credential, inputPassword)) {
            throw new InvalidCredentialsException("Invalid admin login ID or password.");
        }

        Instant now = Instant.now();
        boolean newUser = false;
        User user;

        Optional<AuthIdentity> existingIdentity = authIdentityRepository
            .findByProviderAndProviderSubject(PROVIDER_LOCAL_ADMIN, credential.loginId());

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
                credential.loginId(),
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

    private EffectiveCredential resolveEffectiveCredential() {
        Optional<AdminPasswordCredential> storedCredential = adminPasswordCredentialRepository.find();
        if (storedCredential.isPresent()) {
            AdminPasswordCredential credential = storedCredential.get();
            return EffectiveCredential.fromStored(credential.loginId(), credential.passwordHash());
        }

        if (configuredLoginId.isBlank() || configuredPassword.isBlank()) {
            throw new LoginDisabledException("Admin ID/password login is not configured.");
        }

        return EffectiveCredential.fromConfigured(configuredLoginId, configuredPassword);
    }

    private boolean verifyPassword(EffectiveCredential credential, String inputPassword) {
        if (credential.fromConfigured()) {
            return secureEquals(credential.secret(), inputPassword);
        }
        return passwordEncoder.matches(inputPassword, credential.secret());
    }

    private static boolean secureEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private record EffectiveCredential(String loginId, String secret, boolean fromConfigured) {
        static EffectiveCredential fromConfigured(String loginId, String password) {
            return new EffectiveCredential(loginId, password, true);
        }

        static EffectiveCredential fromStored(String loginId, String passwordHash) {
            return new EffectiveCredential(loginId, passwordHash, false);
        }
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
