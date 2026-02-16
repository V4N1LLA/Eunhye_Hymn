package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.AssetType;
import com.eunhyehymn.domain.model.PartType;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetEntity;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnEntity;
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
class AdminHymnApiTest {
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
        userJpaRepository.save(new UserEntity(adminId, "admin", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()));
        userJpaRepository.save(new UserEntity(userId, "member", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()));

        adminToken = jwtService.issueAccessToken(adminId.toString(), Role.ADMIN.name());
        userToken = jwtService.issueAccessToken(userId.toString(), Role.USER.name());
    }

    @Test
    void adminEndpointRequiresAdminRole() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("title", "forbidden"));

        mockMvc.perform(post("/admin/hymns")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("forbidden"));
    }

    @Test
    void adminCanCreateAndUpdateHymn() throws Exception {
        String createPayload = objectMapper.writeValueAsString(Map.of(
            "title", "create hymn",
            "number", "101",
            "tags", "admin",
            "enabled", false
        ));

        String createResponse = mockMvc.perform(post("/admin/hymns")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("create hymn"))
            .andExpect(jsonPath("$.data.enabled").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String hymnId = objectMapper.readTree(createResponse).get("data").get("id").asText();

        String updatePayload = objectMapper.writeValueAsString(Map.of(
            "title", "update hymn",
            "enabled", true
        ));

        mockMvc.perform(patch("/admin/hymns/" + hymnId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("update hymn"))
            .andExpect(jsonPath("$.data.enabled").value(true));

        HymnEntity saved = hymnJpaRepository.findById(UUID.fromString(hymnId)).orElseThrow();
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void updateWithEmptyPayloadReturnsBadRequest() throws Exception {
        UUID hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "hymn", "1", "tag", true, Instant.now()));

        mockMvc.perform(patch("/admin/hymns/" + hymnId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("empty_update"));
    }

    @Test
    void createTrimsAndNormalizesOptionalFields() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "title", "  normalized hymn  ",
            "number", "   ",
            "tags", "  worship  "
        ));

        mockMvc.perform(post("/admin/hymns")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("normalized hymn"))
            .andExpect(jsonPath("$.data.number").isEmpty())
            .andExpect(jsonPath("$.data.tags").value("worship"));
    }

    @Test
    void adminListReturnsAllHymns() throws Exception {
        hymnJpaRepository.save(new HymnEntity(UUID.randomUUID(), "enabled", "1", "tag", true, Instant.now()));
        hymnJpaRepository.save(new HymnEntity(UUID.randomUUID(), "disabled", "2", "tag", false, Instant.now()));

        String response = mockMvc.perform(get("/admin/hymns")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

        boolean hasDisabled = false;
        for (var item : objectMapper.readTree(response).get("data")) {
            if (!item.get("enabled").asBoolean()) {
                hasDisabled = true;
            }
        }
        assertThat(hasDisabled).isTrue();
    }

    @Test
    void adminCanDeleteHymn() throws Exception {
        UUID hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "delete target", "99", "tag", true, Instant.now()));

        assetJpaRepository.save(new AssetEntity(
            UUID.randomUUID(), hymnId, AssetType.PNG, PartType.ALL,
            "https://example.com/test.png", "hymns/" + hymnId + "/PNG/ALL/test.png",
            null, null, Instant.now()
        ));

        mockMvc.perform(delete("/admin/hymns/" + hymnId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(hymnJpaRepository.findById(hymnId)).isEmpty();
        assertThat(assetJpaRepository.findByHymnIdOrderByCreatedAtAscIdAsc(hymnId)).isEmpty();
    }

    @Test
    void deleteNonExistentHymnReturns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/admin/hymns/" + nonExistentId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("hymn_not_found"));
    }

    @Test
    void deleteHymnRequiresAdminRole() throws Exception {
        UUID hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "protected", "100", "tag", true, Instant.now()));

        mockMvc.perform(delete("/admin/hymns/" + hymnId)
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("forbidden"));

        assertThat(hymnJpaRepository.findById(hymnId)).isPresent();
    }
}
