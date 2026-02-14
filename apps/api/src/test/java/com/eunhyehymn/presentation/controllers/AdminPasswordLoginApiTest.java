package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityEntity;
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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "security.admin.login-id=owner",
    "security.admin.login-password=test-password-123!"
})
class AdminPasswordLoginApiTest {
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
    void firstLoginCreatesAdminUserAndIssuesTokens() throws Exception {
        String response = login("owner", "test-password-123!");

        JsonNode data = objectMapper.readTree(response).get("data");
        String accessToken = data.get("accessToken").asText();
        assertThat(data.get("newUser").asBoolean()).isTrue();

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"));

        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(authIdentityJpaRepository.count()).isEqualTo(1);
        AuthIdentityEntity identity = authIdentityJpaRepository.findAll().get(0);
        assertThat(identity.getProvider()).isEqualTo("LOCAL_ADMIN");
        assertThat(identity.getProviderSubject()).isEqualTo("owner");
    }

    @Test
    void secondLoginUsesExistingAdminUser() throws Exception {
        login("owner", "test-password-123!");
        String response = login("owner", "test-password-123!");

        JsonNode data = objectMapper.readTree(response).get("data");
        assertThat(data.get("newUser").asBoolean()).isFalse();
        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(authIdentityJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidCredentialsReturnsUnauthorized() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "loginId", "owner",
            "password", "wrong-password"
        ));

        mockMvc.perform(post("/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("admin_login_failed"));
    }

    private String login(String loginId, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "loginId", loginId,
            "password", password
        ));

        return mockMvc.perform(post("/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }
}

