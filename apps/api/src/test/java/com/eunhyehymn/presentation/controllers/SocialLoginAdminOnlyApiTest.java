package com.eunhyehymn.presentation.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.application.ports.SocialTokenVerifier;
import com.eunhyehymn.application.ports.SocialUserInfo;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnNoteJpaRepository;
import com.eunhyehymn.infrastructure.persistence.InviteCodeJpaRepository;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "security.admin.kakao-subjects=kakao-admin-subject-1",
    "security.admin.enforce-admin-only=true"
})
class SocialLoginAdminOnlyApiTest {
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
    void allowlistedKakaoCanLoginAndBecomesAdminWithoutInviteCode() throws Exception {
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("valid-kakao-token")))
            .thenReturn(new SocialUserInfo("kakao-admin-subject-1", null, "Operator"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
        request.put("token", "valid-kakao-token");

        String response = mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.newUser").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode data = objectMapper.readTree(response).get("data");
        String accessToken = data.get("accessToken").asText();

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void nonAllowlistedUserIsForbidden() throws Exception {
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("valid-kakao-token")))
            .thenReturn(new SocialUserInfo("someone-else", null, "Someone"));

        Map<String, String> request = new HashMap<>();
        request.put("provider", "KAKAO");
        request.put("token", "valid-kakao-token");

        mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("admin_only"));
    }
}

