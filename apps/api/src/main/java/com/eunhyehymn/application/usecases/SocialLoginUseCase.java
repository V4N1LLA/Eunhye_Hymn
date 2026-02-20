package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.SocialTokenVerifier;
import com.eunhyehymn.application.ports.SocialUserInfo;
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
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class SocialLoginUseCase {
    private final SocialTokenVerifier socialTokenVerifier;
    private final AuthIdentityRepository authIdentityRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final long refreshTokenTtlSeconds;
    private final Set<String> adminEmails;
    private final Set<String> adminKakaoSubjects;
    private final boolean enforceAdminOnly;

    public SocialLoginUseCase(
        SocialTokenVerifier socialTokenVerifier,
        AuthIdentityRepository authIdentityRepository,
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        long refreshTokenTtlSeconds,
        Set<String> adminEmails,
        Set<String> adminKakaoSubjects,
        boolean enforceAdminOnly
    ) {
        this.socialTokenVerifier = socialTokenVerifier;
        this.authIdentityRepository = authIdentityRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.adminEmails = adminEmails != null ? Collections.unmodifiableSet(new HashSet<>(adminEmails)) : Set.of();
        this.adminKakaoSubjects = adminKakaoSubjects != null
            ? Collections.unmodifiableSet(new HashSet<>(adminKakaoSubjects))
            : Set.of();
        this.enforceAdminOnly = enforceAdminOnly;
    }

    @Transactional
    public LoginResult login(String provider, String token, String inviteCode) {
        String normalizedProvider = provider.toUpperCase();
        SocialUserInfo userInfo = socialTokenVerifier.verify(normalizedProvider, token);

        boolean isAdminCandidate = isAdminCandidate(normalizedProvider, userInfo);
        if (enforceAdminOnly && !isAdminCandidate) {
            throw new AdminOnlyException("Only allowlisted admin accounts can log in.");
        }

        Optional<AuthIdentity> existingIdentity = authIdentityRepository
            .findByProviderAndProviderSubject(normalizedProvider, userInfo.providerSubject());

        Instant now = Instant.now();
        User user;

        if (existingIdentity.isPresent()) {
            User existing = userRepository.findById(existingIdentity.get().userId())
                .orElseThrow(() -> new IllegalStateException(
                    "Auth identity has no matching user: " + existingIdentity.get().userId()
                ));
            if (existing.status() != UserStatus.ACTIVE) {
                throw new AccountDisabledException("Disabled account.");
            }

            Role effectiveRole = existing.role();
            if (isAdminCandidate && existing.role() != Role.ADMIN) {
                effectiveRole = Role.ADMIN;
            }

            user = new User(
                existing.id(),
                existing.displayName(),
                effectiveRole,
                existing.status(),
                existing.createdAt(),
                now
            );
            userRepository.save(user);
        } else {
            UUID userId = UUID.randomUUID();
            String displayName = userInfo.displayName() != null ? userInfo.displayName() : normalizedProvider + " User";
            Role role = isAdminCandidate ? Role.ADMIN : Role.USER;
            user = new User(userId, displayName, role, UserStatus.ACTIVE, now, now);
            userRepository.save(user);

            AuthIdentity identity = new AuthIdentity(
                UUID.randomUUID(),
                userId,
                normalizedProvider,
                userInfo.providerSubject(),
                userInfo.email(),
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

        return new LoginResult(accessToken, refreshToken, existingIdentity.isEmpty());
    }

    private boolean isAdminCandidate(String provider, SocialUserInfo userInfo) {
        if (userInfo == null) {
            return false;
        }

        if (userInfo.email() != null && adminEmails.contains(userInfo.email().trim().toLowerCase())) {
            return true;
        }

        if ("KAKAO".equals(provider)) {
            return adminKakaoSubjects.contains(userInfo.providerSubject());
        }

        return false;
    }

    public record LoginResult(String accessToken, String refreshToken, boolean newUser) {
    }

    public static class AdminOnlyException extends RuntimeException {
        public AdminOnlyException(String message) {
            super(message);
        }
    }

    public static class AccountDisabledException extends RuntimeException {
        public AccountDisabledException(String message) {
            super(message);
        }
    }
}
