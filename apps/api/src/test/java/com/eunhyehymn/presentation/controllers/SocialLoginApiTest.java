package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
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
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "SOCIAL-TEST-CODE", null, "test", null, 0, true, null, Instant.now()
        ));

        when(socialTokenVerifier.verify(eq("KAKAO"), eq("valid-kakao-token")))
            .thenReturn(new SocialUserInfo("kakao-sub-123", "test@kakao.com", "Test User"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
        request.put("token", "valid-kakao-token");
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

        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(authIdentityJpaRepository.count()).isEqualTo(1);

        AuthIdentityEntity identity = authIdentityJpaRepository.findAll().get(0);
        assertThat(identity.getProvider()).isEqualTo("KAKAO");
        assertThat(identity.getProviderSubject()).isEqualTo("kakao-sub-123");
        assertThat(identity.getEmail()).isEqualTo("test@kakao.com");

        JsonNode data = objectMapper.readTree(response).get("data");
        String accessToken = data.get("accessToken").asText();

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void existingUserCanLoginWithoutInviteCode() throws Exception {
        UUID userId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            userId, "Existing User", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()
        ));
        authIdentityJpaRepository.save(new AuthIdentityEntity(
            UUID.randomUUID(), userId, "KAKAO", "kakao-id-456", "existing@kakao.com", Instant.now()
        ));

        when(socialTokenVerifier.verify(eq("KAKAO"), eq("valid-kakao-token")))
            .thenReturn(new SocialUserInfo("kakao-id-456", "existing@kakao.com", "Existing User"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
        request.put("token", "valid-kakao-token");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.newUser").value(false));

        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(authIdentityJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void newUserWithoutInviteCodeIsForbidden() throws Exception {
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("valid-kakao-token")))
            .thenReturn(new SocialUserInfo("kakao-new-sub", "new@kakao.com", "New User"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
        request.put("token", "valid-kakao-token");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("invalid_invite_code"));
    }

    @Test
    void invalidSocialTokenReturnsUnauthorized() throws Exception {
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("invalid-token")))
            .thenThrow(new SocialLoginException("Kakao token verification failed: HTTP 400"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
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
    void providerIsCaseInsensitive() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "CASE-CODE", null, "case", null, 0, true, null, Instant.now()
        ));

        when(socialTokenVerifier.verify(eq("KAKAO"), eq("case-token")))
            .thenReturn(new SocialUserInfo("case-sub", "case@kakao.com", "Case User"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "kakao");
        request.put("token", "case-token");
        request.put("inviteCode", "CASE-CODE");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.newUser").value(true));
    }
}