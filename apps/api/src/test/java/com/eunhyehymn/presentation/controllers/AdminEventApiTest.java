package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.model.PartType;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventEntity;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEventApiTest {
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
    private UUID userAId;
    private UUID userBId;
    private UUID hymnAId;
    private UUID hymnBId;
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

        adminId = UUID.randomUUID();
        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();
        hymnAId = UUID.randomUUID();
        hymnBId = UUID.randomUUID();

        userJpaRepository.save(new UserEntity(adminId, "Admin", Role.ADMIN, UserStatus.ACTIVE, Instant.now(), Instant.now()));
        userJpaRepository.save(new UserEntity(userAId, "User A", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()));
        userJpaRepository.save(new UserEntity(userBId, "User B", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now()));

        hymnJpaRepository.save(new HymnEntity(hymnAId, "Hymn A", "101", "tag", true, Instant.now()));
        hymnJpaRepository.save(new HymnEntity(hymnBId, "Hymn B", "102", "tag", true, Instant.now()));

        adminToken = jwtService.issueAccessToken(adminId.toString(), Role.ADMIN.name());
        userToken = jwtService.issueAccessToken(userAId.toString(), Role.USER.name());
    }

    @Test
    void adminEventEndpointRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListEventsAndSummary() throws Exception {
        Instant now = Instant.now();
        UUID latestId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        saveEvent(UUID.randomUUID(), userAId, EventType.PART_PLAYED, hymnAId, PartType.A, now.minus(10, ChronoUnit.DAYS));
        saveEvent(secondId, userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(2, ChronoUnit.HOURS));
        saveEvent(latestId, userBId, EventType.NOTE_SAVED, hymnBId, null, now.minus(1, ChronoUnit.HOURS));

        String response = mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "2")
                .param("summaryDays", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.summary.total").value(2))
            .andExpect(jsonPath("$.data.pagination.page").value(1))
            .andExpect(jsonPath("$.data.pagination.size").value(2))
            .andExpect(jsonPath("$.data.pagination.total").value(3))
            .andExpect(jsonPath("$.data.pagination.totalPages").value(2))
            .andExpect(jsonPath("$.data.pagination.hasPrevious").value(false))
            .andExpect(jsonPath("$.data.pagination.hasNext").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode data = objectMapper.readTree(response).get("data");
        assertThat(data.get("items").get(0).get("id").asText()).isEqualTo(latestId.toString());
        assertThat(data.get("items").get(1).get("id").asText()).isEqualTo(secondId.toString());

        Map<String, Long> byType = new HashMap<>();
        for (JsonNode row : data.get("summary").get("byType")) {
            byType.put(row.get("eventType").asText(), row.get("count").asLong());
        }

        assertThat(byType).containsEntry("HYMN_OPENED", 1L);
        assertThat(byType).containsEntry("NOTE_SAVED", 1L);
        assertThat(byType).containsEntry("PART_PLAYED", 0L);
        assertThat(byType).containsEntry("FAVORITE_TOGGLED", 0L);
    }

    @Test
    void adminCanFilterEvents() throws Exception {
        Instant now = Instant.now();
        saveEvent(UUID.randomUUID(), userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(2, ChronoUnit.HOURS));
        saveEvent(UUID.randomUUID(), userAId, EventType.NOTE_SAVED, hymnAId, null, now.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("eventType", "HYMN_OPENED")
                .param("userId", userAId.toString())
                .param("from", now.minus(1, ChronoUnit.DAYS).toString())
                .param("to", now.plus(1, ChronoUnit.DAYS).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].eventType").value("HYMN_OPENED"))
            .andExpect(jsonPath("$.data.items[0].userId").value(userAId.toString()));
    }

    @Test
    void adminCanNavigateWithPagination() throws Exception {
        Instant now = Instant.now();
        saveEvent(UUID.randomUUID(), userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(3, ChronoUnit.MINUTES));
        saveEvent(UUID.randomUUID(), userAId, EventType.NOTE_SAVED, hymnAId, null, now.minus(2, ChronoUnit.MINUTES));
        saveEvent(UUID.randomUUID(), userAId, EventType.PART_PLAYED, hymnAId, PartType.S, now.minus(1, ChronoUnit.MINUTES));

        mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "2")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.pagination.page").value(2))
            .andExpect(jsonPath("$.data.pagination.size").value(2))
            .andExpect(jsonPath("$.data.pagination.total").value(3))
            .andExpect(jsonPath("$.data.pagination.totalPages").value(2))
            .andExpect(jsonPath("$.data.pagination.hasPrevious").value(true))
            .andExpect(jsonPath("$.data.pagination.hasNext").value(false));
    }

    @Test
    void adminCanExportEventsCsv() throws Exception {
        Instant now = Instant.now();
        saveEvent(UUID.randomUUID(), userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(2, ChronoUnit.MINUTES));
        saveEvent(UUID.randomUUID(), userAId, EventType.NOTE_SAVED, hymnAId, null, now.minus(1, ChronoUnit.MINUTES));

        String csv = mockMvc.perform(get("/admin/events/export")
                .header("Authorization", "Bearer " + adminToken)
                .param("eventType", "HYMN_OPENED")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment;")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(csv).contains("id,userId,eventType,hymnId,part,metadataJson,createdAt");
        assertThat(csv).contains("HYMN_OPENED");
        assertThat(csv).doesNotContain("NOTE_SAVED");
    }

    @Test
    void invalidUserIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("userId", "invalid-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("invalid_uuid"));
    }

    @Test
    void invalidDateRangeReturnsBadRequest() throws Exception {
        Instant now = Instant.now();

        mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("from", now.toString())
                .param("to", now.minus(1, ChronoUnit.HOURS).toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("invalid_range"));
    }

    private void saveEvent(
        UUID id,
        UUID userId,
        EventType type,
        UUID hymnId,
        PartType part,
        Instant createdAt
    ) {
        eventJpaRepository.save(new EventEntity(
            id,
            userId,
            type,
            hymnId,
            part,
            "{\"source\":\"test\"}",
            createdAt
        ));
    }
}
