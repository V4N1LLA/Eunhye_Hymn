package com.eunhyehymn.infrastructure.security;

import com.eunhyehymn.application.ports.SocialTokenVerifier;
import com.eunhyehymn.application.ports.SocialUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class SocialTokenVerifierImpl implements SocialTokenVerifier {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SocialTokenVerifierImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public SocialUserInfo verify(String provider, String token) {
        if (provider == null || provider.isBlank()) {
            throw new SocialLoginException("Unsupported social provider: " + provider);
        }

        return switch (provider.trim().toUpperCase(Locale.ROOT)) {
            case "KAKAO" -> verifyKakao(token);
            default -> throw new SocialLoginException("Unsupported social provider: " + provider);
        };
    }

    private SocialUserInfo verifyKakao(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://kapi.kakao.com/v2/user/me"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new SocialLoginException("Kakao token verification failed: HTTP " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());

            String id = String.valueOf(body.path("id").asLong());
            if ("0".equals(id)) {
                throw new SocialLoginException("Could not extract user id from Kakao token.");
            }

            JsonNode kakaoAccount = body.path("kakao_account");
            String email = kakaoAccount.path("email").asText(null);

            JsonNode profile = kakaoAccount.path("profile");
            String nickname = profile.path("nickname").asText("Kakao User");

            return new SocialUserInfo(id, email, nickname);
        } catch (SocialLoginException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new SocialLoginException("Kakao token verification error: " + e.getMessage());
        }
    }
}
