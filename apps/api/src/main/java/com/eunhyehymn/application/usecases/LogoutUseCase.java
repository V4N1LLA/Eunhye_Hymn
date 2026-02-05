package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;

public class LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, TokenHashService tokenHashService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
    }

    public void logout(String rawRefreshToken) {
        String hash = tokenHashService.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "refresh_invalid", "리프레시 토큰이 유효하지 않습니다", null));

        if (existing.revokedAt() != null) {
            return;
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
    }
}
