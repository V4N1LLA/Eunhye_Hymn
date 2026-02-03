package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.application.ports.TokenService;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final long refreshTokenTtlSeconds;

    public RefreshTokenUseCase(
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        long refreshTokenTtlSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public TokenPair refresh(String rawRefreshToken) {
        String hash = tokenHashService.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "refresh_invalid", "리프레시 토큰이 유효하지 않습니다", null));

        if (existing.revokedAt() != null || existing.expiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "refresh_invalid", "리프레시 토큰이 만료되었습니다", null);
        }

        RefreshToken revoked = new RefreshToken(
            existing.id(),
            existing.userId(),
            existing.tokenHash(),
            existing.expiresAt(),
            Instant.now(),
            existing.createdAt()
        );
        refreshTokenRepository.save(revoked);

        String newRefresh = tokenService.issueRefreshToken();
        String newHash = tokenHashService.hash(newRefresh);
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenTtlSeconds);
        RefreshToken next = new RefreshToken(UUID.randomUUID(), existing.userId(), newHash, expiresAt, null, Instant.now());
        refreshTokenRepository.save(next);

        var user = userRepository.findById(existing.userId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "refresh_invalid", "사용자를 찾을 수 없습니다", null));
        String accessToken = tokenService.issueAccessToken(existing.userId().toString(), user.role().name());

        return new TokenPair(accessToken, newRefresh);
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }
}
