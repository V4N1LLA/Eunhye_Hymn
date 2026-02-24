package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
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
class AdminInviteCodeApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private HymnJpaRepository hymnJpaRepository;
    @Autowired private AssetJpaRepository assetJpaRepository;
    @Autowired private HymnNoteJpaRepository hymnNoteJpaRepository;
    @Autowired private UserHymnStateJpaRepository userHymnStateJpaRepository;
    @Autowired private EventJpaRepository eventJpaRepository;
    @Autowired private AuthIdentityJpaRepository authIdentityJpaRepository;
    @Autowired private RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired private InviteCodeJpaRepository inviteCodeJpaRepository;

    private String adminToken;
    private String userToken;

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

        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(adminId, "관리자", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()));
        userJpaRepository.save(new UserEntity(userId, "일반", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()));

        adminToken = jwtService.issueAccessToken(adminId.toString(), Role.ADMIN.name());
        userToken = jwtService.issueAccessToken(userId.toString(), Role.USER.name());
    }

    @Test
    void inviteCodeEndpointRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/invite-codes")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndListInviteCode() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "code", "TEST-CODE-001",
            "description", "테스트 초대코드"
        ));

        mockMvc.perform(post("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.code").value("TEST-CODE-001"))
            .andExpect(jsonPath("$.data.enabled").value(true))
            .andExpect(jsonPath("$.data.usedCount").value(0));

        mockMvc.perform(get("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].code").value("TEST-CODE-001"));
    }

    @Test
    void adminCanRevokeInviteCode() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("code", "REVOKE-ME"));

        mockMvc.perform(post("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/admin/invite-codes/REVOKE-ME")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].enabled").value(false));
    }

    @Test
    void duplicateCodeReturnsConflict() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("code", "DUP-CODE"));

        mockMvc.perform(post("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict());
    }

    @Test
    void duplicateCodeReturnsConflictEvenWhenLegacyCodeHasDifferentCase() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "legacy-code", null, "legacy", null, 0, true, null, Instant.now()
        ));
        String payload = objectMapper.writeValueAsString(Map.of("code", "LEGACY-CODE"));

        mockMvc.perform(post("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict());
    }

    @Test
    void validateInviteCodePublicEndpoint() throws Exception {
        // Create a code first
        String createPayload = objectMapper.writeValueAsString(Map.of("code", "VALID-CODE"));
        mockMvc.perform(post("/admin/invite-codes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isOk());

        // Validate - no auth needed
        String validatePayload = objectMapper.writeValueAsString(Map.of("code", "VALID-CODE"));
        mockMvc.perform(post("/auth/invite/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(true));

        // Invalid code
        String invalidPayload = objectMapper.writeValueAsString(Map.of("code", "NONEXISTENT"));
        mockMvc.perform(post("/auth/invite/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(false));
    }
}
