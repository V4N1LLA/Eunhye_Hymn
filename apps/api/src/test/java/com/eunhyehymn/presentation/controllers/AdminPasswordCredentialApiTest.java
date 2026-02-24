package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.infrastructure.persistence.AdminPasswordCredentialJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnNoteJpaRepository;
import com.eunhyehymn.infrastructure.persistence.InviteCodeJpaRepository;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
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
class AdminPasswordCredentialApiTest {
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
    @Autowired private AdminPasswordCredentialJpaRepository adminPasswordCredentialJpaRepository;

    @BeforeEach
    void setUp() {
        inviteCodeJpaRepository.deleteAll();
        eventJpaRepository.deleteAll();
        userHymnStateJpaRepository.deleteAll();
        hymnNoteJpaRepository.deleteAll();
        assetJpaRepository.deleteAll();
        authIdentityJpaRepository.deleteAll();
        adminPasswordCredentialJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void adminCanRotateLoginIdAndPassword() throws Exception {
        String token = loginAndExtractAccessToken("owner", "test-password-123!");

        String updatePayload = objectMapper.writeValueAsString(Map.of(
            "currentPassword", "test-password-123!",
            "newLoginId", "owner2",
            "newPassword", "new-password-456!"
        ));

        mockMvc.perform(post("/admin/auth/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.loginId").value("owner2"));

        mockMvc.perform(post("/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "loginId", "owner",
                    "password", "test-password-123!"
                ))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("admin_login_failed"));

        mockMvc.perform(post("/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "loginId", "owner2",
                    "password", "new-password-456!"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void changePasswordRequiresValidCurrentPassword() throws Exception {
        String token = loginAndExtractAccessToken("owner", "test-password-123!");

        String updatePayload = objectMapper.writeValueAsString(Map.of(
            "currentPassword", "wrong-current-password",
            "newLoginId", "owner2",
            "newPassword", "new-password-456!"
        ));

        mockMvc.perform(post("/admin/auth/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("admin_password_change_failed"));
    }

    private String loginAndExtractAccessToken(String loginId, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "loginId", loginId,
            "password", password
        ));

        String body = mockMvc.perform(post("/auth/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }
}
