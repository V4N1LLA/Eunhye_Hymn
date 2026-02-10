package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
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
class AdminUserApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private String adminToken;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        userJpaRepository.deleteAll();

        UUID adminId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            adminId, "관리자", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()
        ));
        adminToken = jwtService.issueAccessToken(adminId.toString(), Role.ADMIN.name());

        targetUserId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            targetUserId, "일반회원", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()
        ));
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void adminCanChangeUserRole() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("role", "ADMIN"));
        mockMvc.perform(patch("/admin/users/" + targetUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void adminCanDisableUser() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("status", "DISABLED"));
        mockMvc.perform(patch("/admin/users/" + targetUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DISABLED"));
    }

    @Test
    void invalidRoleReturnsBadRequest() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("role", "INVALID"));
        mockMvc.perform(patch("/admin/users/" + targetUserId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("invalid_role"));
    }

    @Test
    void nonAdminCannotAccessUserList() throws Exception {
        String userToken = jwtService.issueAccessToken(targetUserId.toString(), Role.USER.name());
        mockMvc.perform(get("/admin/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }
}
