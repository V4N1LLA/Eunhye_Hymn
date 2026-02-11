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
        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> verifyGoogle(token);
            case "KAKAO" -> verifyKakao(token);
            default -> throw new SocialLoginException("지원하지 않는 소셜 로그인 provider: " + provider);
        };
    }

    private SocialUserInfo verifyGoogle(String idToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new SocialLoginException("Google 토큰 검증 실패: HTTP " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());

            String sub = body.path("sub").asText(null);
            String email = body.path("email").asText(null);
            String name = body.path("name").asText("Google User");

            if (sub == null || sub.isBlank()) {
                throw new SocialLoginException("Google 토큰에서 사용자 ID를 추출할 수 없습니다");
            }

            return new SocialUserInfo(sub, email, name);
        } catch (SocialLoginException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new SocialLoginException("Google 토큰 검증 중 오류 발생: " + e.getMessage());
        }
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
                throw new SocialLoginException("Kakao 토큰 검증 실패: HTTP " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());

            String id = String.valueOf(body.path("id").asLong());
            if ("0".equals(id)) {
                throw new SocialLoginException("Kakao 토큰에서 사용자 ID를 추출할 수 없습니다");
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
            throw new SocialLoginException("Kakao 토큰 검증 중 오류 발생: " + e.getMessage());
        }
    }
}
