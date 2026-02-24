package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.model.EventExportJobStatus;
import com.eunhyehymn.domain.model.PartType;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventEntity;
import com.eunhyehymn.infrastructure.persistence.EventExportJobCleanupRunEntity;
import com.eunhyehymn.infrastructure.persistence.EventExportJobCleanupRunJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventExportJobJpaRepository;
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
import java.util.concurrent.TimeUnit;
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
    @Autowired private EventExportJobCleanupRunJpaRepository eventExportJobCleanupRunJpaRepository;
    @Autowired private EventExportJobJpaRepository eventExportJobJpaRepository;
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
        eventExportJobCleanupRunJpaRepository.deleteAll();
        eventExportJobJpaRepository.deleteAll();
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
    void summaryUsesSameFiltersAndWindow() throws Exception {
        Instant now = Instant.now();
        saveEvent(UUID.randomUUID(), userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(10, ChronoUnit.DAYS));
        saveEvent(UUID.randomUUID(), userBId, EventType.NOTE_SAVED, hymnBId, null, now.minus(1, ChronoUnit.HOURS));

        String response = mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("userId", userBId.toString())
                .param("summaryDays", "7"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode summary = objectMapper.readTree(response).get("data").get("summary");
        assertThat(summary.get("total").asLong()).isEqualTo(1L);

        Map<String, Long> counts = new HashMap<>();
        for (JsonNode node : summary.get("byType")) {
            counts.put(node.get("eventType").asText(), node.get("count").asLong());
        }

        assertThat(counts.get("NOTE_SAVED")).isEqualTo(1L);
        assertThat(counts.get("HYMN_OPENED")).isEqualTo(0L);
    }

    @Test
    void summarySupportsCustomSummaryDaysWindow() throws Exception {
        Instant now = Instant.now();
        saveEvent(UUID.randomUUID(), userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(40, ChronoUnit.DAYS));
        saveEvent(UUID.randomUUID(), userAId, EventType.NOTE_SAVED, hymnAId, null, now.minus(20, ChronoUnit.DAYS));

        String within30Days = mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("summaryDays", "30"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String within45Days = mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("summaryDays", "45"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long total30 = objectMapper.readTree(within30Days).get("data").get("summary").get("total").asLong();
        long total45 = objectMapper.readTree(within45Days).get("data").get("summary").get("total").asLong();

        assertThat(total30).isEqualTo(1L);
        assertThat(total45).isEqualTo(2L);
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
    void adminEventOrderingIsStableWhenCreatedAtIsSame() throws Exception {
        Instant createdAt = Instant.parse("2026-02-14T00:00:00Z");
        UUID smallerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID largerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        saveEvent(smallerId, userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, createdAt);
        saveEvent(largerId, userAId, EventType.NOTE_SAVED, hymnAId, null, createdAt);

        String response = mockMvc.perform(get("/admin/events")
                .header("Authorization", "Bearer " + adminToken)
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode items = objectMapper.readTree(response).get("data").get("items");
        assertThat(items.get(0).get("id").asText()).isEqualTo(largerId.toString());
        assertThat(items.get(1).get("id").asText()).isEqualTo(smallerId.toString());
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
    void syncCsvExportSanitizesSpreadsheetFormulaCells() throws Exception {
        Instant now = Instant.now();
        saveEvent(
            UUID.randomUUID(),
            userAId,
            EventType.HYMN_OPENED,
            hymnAId,
            PartType.ALL,
            now.minus(1, ChronoUnit.MINUTES),
            "=SUM(1,1)"
        );

        String csv = mockMvc.perform(get("/admin/events/export")
                .header("Authorization", "Bearer " + adminToken)
                .param("eventType", "HYMN_OPENED")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(csv).contains("\"'=SUM(1,1)\"");
    }

    @Test
    void adminCanCreateAndDownloadAsyncExportJob() throws Exception {
        Instant now = Instant.now();
        saveEvent(UUID.randomUUID(), userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(2, ChronoUnit.MINUTES));
        saveEvent(UUID.randomUUID(), userAId, EventType.NOTE_SAVED, hymnAId, null, now.minus(1, ChronoUnit.MINUTES));

        String createResponse = mockMvc.perform(post("/admin/events/export-jobs")
                .header("Authorization", "Bearer " + adminToken)
                .param("eventType", "HYMN_OPENED")
                .param("limit", "5000"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.id").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        UUID jobId = UUID.fromString(objectMapper.readTree(createResponse).get("data").get("id").asText());
        JsonNode finalState = waitForJobCompletion(jobId, adminToken);

        assertThat(finalState).isNotNull();
        assertThat(finalState.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(finalState.get("rowCount").asLong()).isEqualTo(1L);
        assertThat(finalState.get("downloadable").asBoolean()).isTrue();

        String csv = mockMvc.perform(get("/admin/events/export-jobs/{jobId}/download", jobId)
                .header("Authorization", "Bearer " + adminToken))
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
    void asyncCsvExportSanitizesSpreadsheetFormulaCells() throws Exception {
        Instant now = Instant.now();
        saveEvent(
            UUID.randomUUID(),
            userAId,
            EventType.HYMN_OPENED,
            hymnAId,
            PartType.ALL,
            now.minus(1, ChronoUnit.MINUTES),
            "@SUM(1,1)"
        );

        String createResponse = mockMvc.perform(post("/admin/events/export-jobs")
                .header("Authorization", "Bearer " + adminToken)
                .param("eventType", "HYMN_OPENED")
                .param("limit", "5000"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

        UUID jobId = UUID.fromString(objectMapper.readTree(createResponse).get("data").get("id").asText());
        JsonNode finalState = waitForJobCompletion(jobId, adminToken);
        assertThat(finalState.get("status").asText()).isEqualTo("COMPLETED");

        String csv = mockMvc.perform(get("/admin/events/export-jobs/{jobId}/download", jobId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(csv).contains("\"'@SUM(1,1)\"");
    }

    @Test
    void asyncExportUsesCreationSnapshotWhenToIsOmitted() throws Exception {
        UUID firstEventId = UUID.randomUUID();
        UUID futureEventId = UUID.randomUUID();
        Instant now = Instant.now();
        saveEvent(firstEventId, userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.minus(1, ChronoUnit.MINUTES));

        String createResponse = mockMvc.perform(post("/admin/events/export-jobs")
                .header("Authorization", "Bearer " + adminToken)
                .param("eventType", "HYMN_OPENED")
                .param("limit", "5000"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

        UUID jobId = UUID.fromString(objectMapper.readTree(createResponse).get("data").get("id").asText());
        saveEvent(futureEventId, userAId, EventType.HYMN_OPENED, hymnAId, PartType.ALL, now.plus(1, ChronoUnit.HOURS));

        JsonNode finalState = waitForJobCompletion(jobId, adminToken);
        assertThat(finalState.get("status").asText()).isEqualTo("COMPLETED");

        String csv = mockMvc.perform(get("/admin/events/export-jobs/{jobId}/download", jobId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(csv).contains(firstEventId.toString());
        assertThat(csv).doesNotContain(futureEventId.toString());
    }

    @Test
    void asyncExportJobIsVisibleOnlyToRequester() throws Exception {
        UUID secondAdminId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            secondAdminId,
            "Admin 2",
            Role.ADMIN,
            UserStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        ));
        String secondAdminToken = jwtService.issueAccessToken(secondAdminId.toString(), Role.ADMIN.name());

        String createResponse = mockMvc.perform(post("/admin/events/export-jobs")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UUID jobId = UUID.fromString(objectMapper.readTree(createResponse).get("data").get("id").asText());

        mockMvc.perform(get("/admin/events/export-jobs/{jobId}", jobId)
                .header("Authorization", "Bearer " + secondAdminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("export_job_not_found"));

        waitForJobCompletion(jobId, adminToken);
    }

    @Test
    void downloadReturnsConflictWhenAsyncExportJobIsNotCompleted() throws Exception {
        UUID queuedJobId = saveExportJob(
            EventExportJobStatus.QUEUED,
            Instant.now().minus(1, ChronoUnit.MINUTES),
            null,
            null
        );

        mockMvc.perform(get("/admin/events/export-jobs/{jobId}/download", queuedJobId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("export_job_not_ready"));
    }

    @Test
    void adminCanGetAsyncExportOpsMetrics() throws Exception {
        Instant now = Instant.now();
        saveExportJob(
            EventExportJobStatus.COMPLETED,
            now.minus(2, ChronoUnit.HOURS),
            now.minus(2, ChronoUnit.HOURS).plusSeconds(10),
            now.minus(2, ChronoUnit.HOURS).plusSeconds(20)
        );
        saveExportJob(
            EventExportJobStatus.FAILED,
            now.minus(90, ChronoUnit.MINUTES),
            now.minus(90, ChronoUnit.MINUTES).plusSeconds(5),
            now.minus(90, ChronoUnit.MINUTES).plusSeconds(35)
        );
        saveExportJob(
            EventExportJobStatus.RUNNING,
            now.minus(1, ChronoUnit.HOURS),
            now.minus(1, ChronoUnit.HOURS).plusSeconds(5),
            null
        );
        saveExportJob(
            EventExportJobStatus.QUEUED,
            now.minus(30, ChronoUnit.MINUTES),
            null,
            null
        );
        saveExportJob(
            EventExportJobStatus.COMPLETED,
            now.minus(20, ChronoUnit.DAYS),
            now.minus(20, ChronoUnit.DAYS).plusSeconds(5),
            now.minus(20, ChronoUnit.DAYS).plusSeconds(30)
        );

        saveCleanupRun(now.minus(1, ChronoUnit.DAYS), 4L);
        saveCleanupRun(now.minus(12, ChronoUnit.DAYS), 20L);

        mockMvc.perform(get("/admin/events/export-jobs/metrics")
                .header("Authorization", "Bearer " + adminToken)
                .param("days", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.windowDays").value(7))
            .andExpect(jsonPath("$.data.jobs.total").value(4))
            .andExpect(jsonPath("$.data.jobs.completed").value(1))
            .andExpect(jsonPath("$.data.jobs.failed").value(1))
            .andExpect(jsonPath("$.data.jobs.failureRatePercent").value(50.0))
            .andExpect(jsonPath("$.data.processing.measuredJobs").value(2))
            .andExpect(jsonPath("$.data.processing.averageSeconds").value(20.0))
            .andExpect(jsonPath("$.data.processing.p95Seconds").value(30.0))
            .andExpect(jsonPath("$.data.cleanup.runCount").value(1))
            .andExpect(jsonPath("$.data.cleanup.deletedJobs").value(4));
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

    private JsonNode waitForJobCompletion(UUID jobId, String token) throws Exception {
        JsonNode latest = null;
        for (int attempt = 0; attempt < 40; attempt += 1) {
            String response = mockMvc.perform(get("/admin/events/export-jobs/{jobId}", jobId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            latest = objectMapper.readTree(response).get("data");
            String status = latest.get("status").asText();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                return latest;
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        return latest;
    }

    private void saveEvent(
        UUID id,
        UUID userId,
        EventType type,
        UUID hymnId,
        PartType part,
        Instant createdAt
    ) {
        saveEvent(id, userId, type, hymnId, part, createdAt, "{\"source\":\"test\"}");
    }

    private void saveEvent(
        UUID id,
        UUID userId,
        EventType type,
        UUID hymnId,
        PartType part,
        Instant createdAt,
        String metadataJson
    ) {
        eventJpaRepository.save(new EventEntity(
            id,
            userId,
            type,
            hymnId,
            part,
            metadataJson,
            createdAt
        ));
    }

    private UUID saveExportJob(
        EventExportJobStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
    ) {
        UUID id = UUID.randomUUID();
        eventExportJobJpaRepository.save(new com.eunhyehymn.infrastructure.persistence.EventExportJobEntity(
            id,
            adminId,
            EventType.HYMN_OPENED,
            userAId,
            hymnAId,
            null,
            null,
            10_000,
            status,
            status == EventExportJobStatus.COMPLETED ? 10L : null,
            status == EventExportJobStatus.COMPLETED ? "test.csv" : null,
            status == EventExportJobStatus.COMPLETED ? "id,userId\n" : null,
            status == EventExportJobStatus.FAILED ? "test failure" : null,
            createdAt,
            startedAt,
            completedAt
        ));
        return id;
    }

    private void saveCleanupRun(Instant executedAt, long deletedCount) {
        eventExportJobCleanupRunJpaRepository.save(new EventExportJobCleanupRunEntity(
            UUID.randomUUID(),
            executedAt,
            7,
            deletedCount
        ));
    }
}
