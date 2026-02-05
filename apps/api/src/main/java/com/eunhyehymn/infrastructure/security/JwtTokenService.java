package com.eunhyehymn.infrastructure.security;

import com.eunhyehymn.application.ports.TokenService;
import java.security.SecureRandom;
import java.util.Base64;

public class JwtTokenService implements TokenService {
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String issueAccessToken(String userId, String role) {
        return jwtService.issueAccessToken(userId, role);
    }

    @Override
    public String issueRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
