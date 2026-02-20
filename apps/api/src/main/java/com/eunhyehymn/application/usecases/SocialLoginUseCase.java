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
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class SocialLoginUseCase {
    private final SocialTokenVerifier socialTokenVerifier;
    private final AuthIdentityRepository authIdentityRepository;
    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
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
        InviteCodeRepository inviteCodeRepository,
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
        this.inviteCodeRepository = inviteCodeRepository;
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
        String normalizedInviteCode = normalizeInviteCode(inviteCode);

        boolean isAdminCandidate = isAdminCandidate(normalizedProvider, userInfo);
        if (enforceAdminOnly && !isAdminCandidate) {
            throw new AdminOnlyException("관리자만 이용 가능한 계정입니다.");
        }

        Optional<AuthIdentity> existingIdentity = authIdentityRepository
            .findByProviderAndProviderSubject(normalizedProvider, userInfo.providerSubject());

        Instant now = Instant.now();
        User user;

        if (existingIdentity.isPresent()) {
            User existing = userRepository.findById(existingIdentity.get().userId())
                .orElseThrow(() -> new IllegalStateException(
                    "AuthIdentity에 해당하는 사용자 조회 실패: " + existingIdentity.get().userId()));

            if (existing.status() == UserStatus.DISABLED) {
                throw new AccountDisabledException("account is disabled");
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
            if (!isAdminCandidate) {
                if (normalizedInviteCode == null || normalizedInviteCode.isBlank()) {
                    throw new InvalidInviteCodeException("초대코드가 비어있습니다");
                }

                inviteCodeRepository.findByCode(normalizedInviteCode)
                    .orElseThrow(() -> new InvalidInviteCodeException("유효하지 않은 초대코드입니다"));

                boolean incremented = inviteCodeRepository.incrementUsedCount(normalizedInviteCode);
                if (!incremented) {
                    throw new InvalidInviteCodeException("유효하지 않은 초대코드입니다");
                }
            }

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

    private String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null) {
            return null;
        }
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }

    public record LoginResult(String accessToken, String refreshToken, boolean newUser) {
    }

    public static class InvalidInviteCodeException extends RuntimeException {
        public InvalidInviteCodeException(String message) {
            super(message);
        }
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
