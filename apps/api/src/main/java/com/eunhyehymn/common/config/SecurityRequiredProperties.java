package com.eunhyehymn.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Profile("!test")
@Validated
@ConfigurationProperties(prefix = "security")
public class SecurityRequiredProperties {
    @Valid
    @NotNull
    private Jwt jwt = new Jwt();

    @Valid
    @NotNull
    private Invite invite = new Invite();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Invite getInvite() {
        return invite;
    }

    public void setInvite(Invite invite) {
        this.invite = invite;
    }

    public static class Jwt {
        @NotBlank(message = "JWT_SECRET는 필수값입니다.")
        private String secret;

        @NotNull(message = "JWT_ACCESS_TTL_SECONDS는 필수값입니다.")
        @Positive(message = "JWT_ACCESS_TTL_SECONDS는 0보다 커야 합니다.")
        private Long accessTokenTtlSeconds;

        @NotNull(message = "JWT_REFRESH_TTL_SECONDS는 필수값입니다.")
        @Positive(message = "JWT_REFRESH_TTL_SECONDS는 0보다 커야 합니다.")
        private Long refreshTokenTtlSeconds;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Long getAccessTokenTtlSeconds() {
            return accessTokenTtlSeconds;
        }

        public void setAccessTokenTtlSeconds(Long accessTokenTtlSeconds) {
            this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        }

        public Long getRefreshTokenTtlSeconds() {
            return refreshTokenTtlSeconds;
        }

        public void setRefreshTokenTtlSeconds(Long refreshTokenTtlSeconds) {
            this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        }
    }

    public static class Invite {
        // DB 기반 초대코드 관리로 전환됨. 환경변수는 선택 사항.
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
