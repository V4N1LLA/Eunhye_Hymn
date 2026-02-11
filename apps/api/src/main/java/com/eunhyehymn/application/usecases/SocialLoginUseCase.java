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
import java.util.Optional;
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

    public SocialLoginUseCase(
        SocialTokenVerifier socialTokenVerifier,
        AuthIdentityRepository authIdentityRepository,
        UserRepository userRepository,
        InviteCodeRepository inviteCodeRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        long refreshTokenTtlSeconds
    ) {
        this.socialTokenVerifier = socialTokenVerifier;
        this.authIdentityRepository = authIdentityRepository;
        this.userRepository = userRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public LoginResult login(String provider, String token, String inviteCode) {
        // 1. 소셜 토큰 검증 → 사용자 정보 추출
        String normalizedProvider = provider.toUpperCase();
        SocialUserInfo userInfo = socialTokenVerifier.verify(normalizedProvider, token);

        // 2. AuthIdentity 조회
        Optional<AuthIdentity> existingIdentity = authIdentityRepository
            .findByProviderAndProviderSubject(normalizedProvider, userInfo.providerSubject());

        Instant now = Instant.now();
        User user;

        if (existingIdentity.isPresent()) {
            // 기존 사용자: 초대코드 검증 불필요, lastLoginAt 갱신
            User existing = userRepository.findById(existingIdentity.get().userId())
                .orElseThrow(() -> new IllegalStateException(
                    "AuthIdentity에 연결된 User를 찾을 수 없습니다: " + existingIdentity.get().userId()));

            user = new User(
                existing.id(),
                existing.displayName(),
                existing.role(),
                existing.status(),
                existing.createdAt(),
                now
            );
            userRepository.save(user);
        } else {
            // 신규 사용자: 초대코드 검증 필수
            if (inviteCode == null || inviteCode.isBlank()) {
                throw new InvalidInviteCodeException("초대코드가 필요합니다");
            }

            // 초대코드 존재 여부 확인
            inviteCodeRepository.findByCode(inviteCode)
                .orElseThrow(() -> new InvalidInviteCodeException("유효하지 않은 초대코드입니다"));

            // 원자적으로 usedCount 증가 (enabled, maxUses, expiresAt 동시 검증)
            boolean incremented = inviteCodeRepository.incrementUsedCount(inviteCode);
            if (!incremented) {
                throw new InvalidInviteCodeException("유효하지 않은 초대코드입니다");
            }

            // User 생성
            UUID userId = UUID.randomUUID();
            String displayName = userInfo.displayName() != null ? userInfo.displayName() : normalizedProvider + " User";
            user = new User(userId, displayName, Role.USER, UserStatus.ACTIVE, now, now);
            userRepository.save(user);

            // AuthIdentity 생성
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

        // 3. JWT 발급
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

    public record LoginResult(String accessToken, String refreshToken, boolean newUser) {
    }

    public static class InvalidInviteCodeException extends RuntimeException {
        public InvalidInviteCodeException(String message) {
            super(message);
        }
    }
}
