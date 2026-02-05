package com.eunhyehymn.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtService {
    private final ObjectMapper objectMapper;
    private final String secret;
    private final long accessTokenTtlSeconds;

    public JwtService(ObjectMapper objectMapper, String secret, long accessTokenTtlSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String issueAccessToken(String userId, String role) {
        long now = Instant.now().getEpochSecond();
        long exp = now + accessTokenTtlSeconds;
        Map<String, Object> payload = Map.of(
            "sub", userId,
            "role", role,
            "iat", now,
            "exp", exp
        );
        return encode(payload);
    }

    public JwtClaims validateAccessToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtValidationException("invalid_format");
        }

        String signature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new JwtValidationException("invalid_signature");
        }

        Map<String, Object> payload = decodePayload(parts[1]);
        long exp = ((Number) payload.get("exp")).longValue();
        if (Instant.now().getEpochSecond() > exp) {
            throw new JwtValidationException("expired");
        }

        return new JwtClaims((String) payload.get("sub"), (String) payload.get("role"));
    }

    private String encode(Map<String, Object> payload) {
        try {
            String header = base64Url(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String body = base64Url(objectMapper.writeValueAsBytes(payload));
            String signature = sign(header + "." + body);
            return header + "." + body + "." + signature;
        } catch (JsonProcessingException ex) {
            throw new JwtValidationException("encode_failed");
        }
    }

    private Map<String, Object> decodePayload(String body) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(body);
            return objectMapper.readValue(decoded, Map.class);
        } catch (Exception ex) {
            throw new JwtValidationException("decode_failed");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return base64Url(signature);
        } catch (Exception ex) {
            throw new JwtValidationException("sign_failed");
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record JwtClaims(String userId, String role) {
    }
}
