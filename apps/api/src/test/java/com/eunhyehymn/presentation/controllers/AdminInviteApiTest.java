package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.InviteCodeJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserEntity;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.eunhyehymn.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
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
class AdminInviteApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private InviteCodeJpaRepository inviteCodeJpaRepository;

    private String adminToken;

    @BeforeEach
    void setUp() {
        inviteCodeJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        UUID adminId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            adminId, "관리자", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()
        ));
        adminToken = jwtService.issueAccessToken(adminId.toString(), Role.ADMIN.name());
    }

    @Test
    void adminCanCreateInviteCode() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "code", "WELCOME2026"
        ));

        mockMvc.perform(post("/admin/invites")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.code").value("WELCOME2026"))
            .andExpect(jsonPath("$.data.usedCount").value(0));
    }

    @Test
    void adminCanListInviteCodes() throws Exception {
        // create one first
        String payload = objectMapper.writeValueAsString(Map.of("code", "LIST_TEST"));
        mockMvc.perform(post("/admin/invites")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/admin/invites")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].code").value("LIST_TEST"));
    }

    @Test
    void adminCanRevokeInviteCode() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("code", "REVOKE_ME"));
        String response = mockMvc.perform(post("/admin/invites")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("data").get("id").asText();

        mockMvc.perform(post("/admin/invites/" + id + "/revoke")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.revokedAt").isNotEmpty());
    }

    @Test
    void validateInviteCodeWorks() throws Exception {
        // create code
        String createPayload = objectMapper.writeValueAsString(Map.of("code", "VALID_CODE"));
        mockMvc.perform(post("/admin/invites")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isOk());

        // validate (public endpoint)
        String validatePayload = objectMapper.writeValueAsString(Map.of("inviteCode", "VALID_CODE"));
        mockMvc.perform(post("/auth/invite/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    void validateRejectsInvalidCode() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("inviteCode", "WRONG"));
        mockMvc.perform(post("/auth/invite/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(false));
    }

    @Test
    void validateRejectsRevokedCode() throws Exception {
        // create and revoke
        String createPayload = objectMapper.writeValueAsString(Map.of("code", "REVOKED"));
        String response = mockMvc.perform(post("/admin/invites")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("data").get("id").asText();

        mockMvc.perform(post("/admin/invites/" + id + "/revoke")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // try validate
        String validatePayload = objectMapper.writeValueAsString(Map.of("inviteCode", "REVOKED"));
        mockMvc.perform(post("/auth/invite/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(false));
    }
}
