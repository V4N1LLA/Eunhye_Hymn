package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnNoteJpaRepository;
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
class AdminUserApiTest {
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

    private UUID adminId;
    private String adminToken;
    private String userToken;
    private UUID userId;

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

        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(adminId, "관리자", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()));
        userJpaRepository.save(new UserEntity(userId, "일반", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()));

        adminToken = jwtService.issueAccessToken(adminId.toString(), Role.ADMIN.name());
        userToken = jwtService.issueAccessToken(userId.toString(), Role.USER.name());
    }

    @Test
    void userEndpointRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void adminCanUpdateUserRole() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("role", "ADMIN"));

        mockMvc.perform(patch("/admin/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            .andExpect(jsonPath("$.data.displayName").value("일반"));
    }

    @Test
    void adminCanDisableUser() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("status", "DISABLED"));

        mockMvc.perform(patch("/admin/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void invalidRoleReturnsBadRequest() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("role", "INVALID"));

        mockMvc.perform(patch("/admin/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateNonExistentUserReturnsNotFound() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("role", "ADMIN"));

        mockMvc.perform(patch("/admin/users/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotDemoteSelf() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("role", "USER"));

        mockMvc.perform(patch("/admin/users/" + adminId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("self_role_change"));
    }

    @Test
    void cannotDemoteLastActiveAdmin() throws Exception {
        // setUp creates one ADMIN (adminId) and one USER (userId).
        // adminId is the only active admin, so demoting them via another admin should fail.
        // Create a second admin to perform the request.
        UUID secondAdminId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(secondAdminId, "두번째관리자", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()));
        String secondAdminToken = jwtService.issueAccessToken(secondAdminId.toString(), Role.ADMIN.name());

        // Now there are 2 active admins. Demoting one should succeed.
        String demotePayload = objectMapper.writeValueAsString(Map.of("role", "USER"));
        mockMvc.perform(patch("/admin/users/" + adminId)
                .header("Authorization", "Bearer " + secondAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demotePayload))
            .andExpect(status().isOk());

        // Now secondAdmin is the only active admin. Use adminToken (demoted but JWT still valid) to try demoting the last admin.
        mockMvc.perform(patch("/admin/users/" + secondAdminId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(demotePayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("last_admin"));
    }
}
