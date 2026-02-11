package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.application.ports.SocialTokenVerifier;
import com.eunhyehymn.application.ports.SocialUserInfo;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityEntity;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnNoteJpaRepository;
import com.eunhyehymn.infrastructure.persistence.InviteCodeEntity;
import com.eunhyehymn.infrastructure.persistence.InviteCodeJpaRepository;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserEntity;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.eunhyehymn.infrastructure.security.SocialLoginException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialLoginApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private HymnJpaRepository hymnJpaRepository;
    @Autowired private AssetJpaRepository assetJpaRepository;
    @Autowired private HymnNoteJpaRepository hymnNoteJpaRepository;
    @Autowired private UserHymnStateJpaRepository userHymnStateJpaRepository;
    @Autowired private EventJpaRepository eventJpaRepository;
    @Autowired private AuthIdentityJpaRepository authIdentityJpaRepository;
    @Autowired private RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired private InviteCodeJpaRepository inviteCodeJpaRepository;

    @MockBean
    private SocialTokenVerifier socialTokenVerifier;

    @BeforeEach
    void setUp() {
        inviteCodeJpaRepository.deleteAll();
        eventJpaRepository.deleteAll();
        userHymnStateJpaRepository.deleteAll();
        hymnNoteJpaRepository.deleteAll();
        assetJpaRepository.deleteAll();
        authIdentityJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void newUserWithValidInviteCodeCreatesAccountAndReturnsTokens() throws Exception {
        // Given: 유효한 초대코드 생성
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "SOCIAL-TEST-CODE", null, "테스트", null, 0, true, null, Instant.now()
        ));

        // Mock: Google 소셜 토큰 검증 성공
        when(socialTokenVerifier.verify(eq("GOOGLE"), eq("valid-google-token")))
            .thenReturn(new SocialUserInfo("google-sub-123", "test@gmail.com", "테스트 사용자"));

        // When
        Map<String, String> request = new HashMap<>();
        request.put("provider", "GOOGLE");
        request.put("token", "valid-google-token");
        request.put("inviteCode", "SOCIAL-TEST-CODE");

        String response = mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.newUser").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Then: User, AuthIdentity 생성 확인
        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(authIdentityJpaRepository.count()).isEqualTo(1);

        AuthIdentityEntity identity = authIdentityJpaRepository.findAll().get(0);
        assertThat(identity.getProvider()).isEqualTo("GOOGLE");
        assertThat(identity.getProviderSubject()).isEqualTo("google-sub-123");
        assertThat(identity.getEmail()).isEqualTo("test@gmail.com");

        // 초대코드 usedCount 증가 확인
        assertThat(inviteCodeJpaRepository.findById("SOCIAL-TEST-CODE").get().getUsedCount()).isEqualTo(1);

        // 발급된 accessToken으로 /me/profile 호출 가능 확인
        JsonNode data = objectMapper.readTree(response).get("data");
        String accessToken = data.get("accessToken").asText();

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void existingUserCanLoginWithoutInviteCode() throws Exception {
        // Given: 기존 사용자와 AuthIdentity 생성
        UUID userId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            userId, "기존 사용자", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()
        ));
        authIdentityJpaRepository.save(new AuthIdentityEntity(
            UUID.randomUUID(), userId, "KAKAO", "kakao-id-456", "existing@kakao.com", Instant.now()
        ));

        // Mock: Kakao 소셜 토큰 검증 성공
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("valid-kakao-token")))
            .thenReturn(new SocialUserInfo("kakao-id-456", "existing@kakao.com", "카카오 사용자"));

        // When: 초대코드 없이 로그인
        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
        request.put("token", "valid-kakao-token");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.newUser").value(false));

        // Then: 새 사용자가 생성되지 않음
        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(authIdentityJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void newUserWithoutInviteCodeIsForbidden() throws Exception {
        // Mock: Google 소셜 토큰 검증 성공
        when(socialTokenVerifier.verify(eq("GOOGLE"), eq("valid-google-token")))
            .thenReturn(new SocialUserInfo("google-new-sub", "new@gmail.com", "신규 사용자"));

        // When: 초대코드 없이 신규 사용자 로그인 시도
        Map<String, String> request = new HashMap<>();
        request.put("provider", "GOOGLE");
        request.put("token", "valid-google-token");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("invalid_invite_code"));
    }

    @Test
    void newUserWithInvalidInviteCodeIsForbidden() throws Exception {
        // Mock: Google 소셜 토큰 검증 성공
        when(socialTokenVerifier.verify(eq("GOOGLE"), eq("valid-google-token")))
            .thenReturn(new SocialUserInfo("google-new-sub-2", "new2@gmail.com", "신규 사용자 2"));

        // When: 잘못된 초대코드로 로그인 시도
        Map<String, String> request = new HashMap<>();
        request.put("provider", "GOOGLE");
        request.put("token", "valid-google-token");
        request.put("inviteCode", "INVALID-CODE");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("invalid_invite_code"));
    }

    @Test
    void invalidSocialTokenReturnsUnauthorized() throws Exception {
        // Mock: 소셜 토큰 검증 실패
        when(socialTokenVerifier.verify(eq("GOOGLE"), eq("invalid-token")))
            .thenThrow(new SocialLoginException("Google 토큰 검증 실패: HTTP 400"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "GOOGLE");
        request.put("token", "invalid-token");
        request.put("inviteCode", "ANY-CODE");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("social_auth_failed"));
    }

    @Test
    void kakaoNewUserFlowWithInviteCode() throws Exception {
        // Given: 유효한 초대코드
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "KAKAO-INVITE", null, "카카오 테스트", null, 0, true, null, Instant.now()
        ));

        // Mock: Kakao 소셜 토큰 검증 성공
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("kakao-access-token")))
            .thenReturn(new SocialUserInfo("kakao-id-789", "kakao@test.com", "카카오 신규 사용자"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
        request.put("token", "kakao-access-token");
        request.put("inviteCode", "KAKAO-INVITE");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.newUser").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // Then
        AuthIdentityEntity identity = authIdentityJpaRepository.findAll().get(0);
        assertThat(identity.getProvider()).isEqualTo("KAKAO");
        assertThat(identity.getProviderSubject()).isEqualTo("kakao-id-789");

        UserEntity user = userJpaRepository.findAll().get(0);
        assertThat(user.getDisplayName()).isEqualTo("카카오 신규 사용자");
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void disabledInviteCodeIsForbidden() throws Exception {
        // Given: 비활성화된 초대코드
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "DISABLED-CODE", null, "비활성", null, 0, false, null, Instant.now()
        ));

        when(socialTokenVerifier.verify(eq("GOOGLE"), eq("valid-token")))
            .thenReturn(new SocialUserInfo("google-sub-disabled", "disabled@gmail.com", "사용자"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "GOOGLE");
        request.put("token", "valid-token");
        request.put("inviteCode", "DISABLED-CODE");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("invalid_invite_code"));
    }

    @Test
    void providerIsCaseInsensitive() throws Exception {
        // Given
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "CASE-CODE", null, "case test", null, 0, true, null, Instant.now()
        ));

        // provider를 소문자로 전달해도 정상 동작
        when(socialTokenVerifier.verify(eq("GOOGLE"), eq("case-token")))
            .thenReturn(new SocialUserInfo("case-sub", "case@gmail.com", "Case User"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "google");
        request.put("token", "case-token");
        request.put("inviteCode", "CASE-CODE");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.newUser").value(true));
    }
}
