package com.eunhyehymn.application.ports;

public interface TokenService {
    String issueAccessToken(String userId, String role);

    String issueRefreshToken();
}
