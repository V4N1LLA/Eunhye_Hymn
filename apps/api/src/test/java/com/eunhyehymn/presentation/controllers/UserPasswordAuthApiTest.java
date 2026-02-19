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
import com.eunhyehymn.infrastructure.persistence.InviteCodeEntity;
import com.eunhyehymn.infrastructure.persistence.InviteCodeJpaRepository;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserPasswordCredentialJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserPasswordAuthApiTest {
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
    @Autowired private UserPasswordCredentialJpaRepository userPasswordCredentialJpaRepository;

    @BeforeEach
    void setUp() {
        inviteCodeJpaRepository.deleteAll();
        eventJpaRepository.deleteAll();
        userHymnStateJpaRepository.deleteAll();
        hymnNoteJpaRepository.deleteAll();
        assetJpaRepository.deleteAll();
        authIdentityJpaRepository.deleteAll();
        userPasswordCredentialJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void signupWithInviteCodeCreatesLocalUserAndIssuesTokens() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "SIGNUP-CODE", null, "signup", null, 0, true, null, Instant.now()
        ));

        String response = signup("Member.One", "password-123!", "  signup-code  ");
        JsonNode data = objectMapper.readTree(response).path("data");
        String accessToken = data.path("accessToken").asText();

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("USER"));

        assertThat(data.path("newUser").asBoolean()).isTrue();
        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(authIdentityJpaRepository.count()).isEqualTo(1);
        assertThat(userPasswordCredentialJpaRepository.count()).isEqualTo(1);

        AuthIdentityEntity identity = authIdentityJpaRepository.findAll().get(0);
        assertThat(identity.getProvider()).isEqualTo("LOCAL_USER");
        assertThat(identity.getProviderSubject()).isEqualTo("member.one");
        assertThat(inviteCodeJpaRepository.findById("SIGNUP-CODE").orElseThrow().getUsedCount()).isEqualTo(1);
    }

    @Test
    void signupRejectsDuplicateLoginIdIgnoringCase() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "CODE-1", null, "code1", null, 0, true, null, Instant.now()
        ));

        signup("Member.One", "password-123!", "code-1");

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "loginId", "member.one",
                    "password", "password-123!",
                    "inviteCode", "code-1"
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("login_id_exists"));
    }

    @Test
    void loginWithCreatedCredentialIssuesTokens() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "LOGIN-CODE", null, "login", null, 0, true, null, Instant.now()
        ));
        signup("service.user", "password-123!", "login-code");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "loginId", "SERVICE.USER",
                    "password", "password-123!"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.newUser").value(false));
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "WRONG-PW-CODE", null, "wrongpw", null, 0, true, null, Instant.now()
        ));
        signup("member.two", "password-123!", "WRONG-PW-CODE");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "loginId", "member.two",
                    "password", "wrong-password"
                ))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("user_login_failed"));
    }

    private String signup(String loginId, String password, String inviteCode) throws Exception {
        return mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "loginId", loginId,
                    "password", password,
                    "inviteCode", inviteCode
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }
}
