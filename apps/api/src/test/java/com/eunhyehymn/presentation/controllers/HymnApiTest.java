package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AssetEntity;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnEntity;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserEntity;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.eunhyehymn.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
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
class HymnApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private HymnJpaRepository hymnJpaRepository;

    @Autowired
    private AssetJpaRepository assetJpaRepository;

    @Autowired
    private UserHymnStateJpaRepository userHymnStateJpaRepository;

    private UUID userId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        userHymnStateJpaRepository.deleteAll();
        assetJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, "테스트", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now());
        userJpaRepository.save(user);

        accessToken = jwtService.issueAccessToken(userId.toString(), Role.USER.name());
    }

    @Test
    void listHymnsIsPublicAndReturnsEnabledOnly() throws Exception {
        hymnJpaRepository.save(new HymnEntity(UUID.randomUUID(), "활성", "1", "tag", true, Instant.now()));
        hymnJpaRepository.save(new HymnEntity(UUID.randomUUID(), "비활성", "2", "tag", false, Instant.now()));

        mockMvc.perform(get("/hymns"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].title").value("활성"));
    }

    @Test
    void detailRequiresAuthAndUpdatesHistory() throws Exception {
        UUID hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "상세", "3", "tag", true, Instant.now()));
        assetJpaRepository.save(new AssetEntity(
            UUID.randomUUID(),
            hymnId,
            com.eunhyehymn.domain.model.AssetType.PDF,
            com.eunhyehymn.domain.model.PartType.ALL,
            "url",
            "hymns/" + hymnId + "/PDF/ALL/asset.pdf",
            null,
            null,
            Instant.now()
        ));

        mockMvc.perform(get("/hymns/" + hymnId))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/hymns/" + hymnId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.assets.length()").value(1));

        var state = userHymnStateJpaRepository.findByIdUserIdAndIdHymnId(userId, hymnId);
        assertThat(state).isPresent();
    }

    @Test
    void favoriteToggleWorks() throws Exception {
        UUID hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "즐겨찾기", "4", "tag", true, Instant.now()));

        mockMvc.perform(post("/me/favorites/" + hymnId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(post("/me/favorites/" + hymnId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorite").value(false));
    }

    @Test
    void noteSaveAndGet() throws Exception {
        UUID hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "노트", "5", "tag", true, Instant.now()));

        String payload = objectMapper.writeValueAsString(Map.of("content", "메모"));
        mockMvc.perform(put("/me/hymns/" + hymnId + "/note")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("메모"));

        mockMvc.perform(get("/me/hymns/" + hymnId + "/note")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").value("메모"));
    }

    @Test
    void historyReturnsNewestFirst() throws Exception {
        UUID hymnA = UUID.randomUUID();
        UUID hymnB = UUID.randomUUID();
        hymnJpaRepository.saveAll(List.of(
            new HymnEntity(hymnA, "첫번째", "6", "tag", true, Instant.now()),
            new HymnEntity(hymnB, "두번째", "7", "tag", true, Instant.now())
        ));

        var now = Instant.now();
        userHymnStateJpaRepository.saveAll(List.of(
            new com.eunhyehymn.infrastructure.persistence.UserHymnStateEntity(
                new com.eunhyehymn.infrastructure.persistence.UserHymnStateId(userId, hymnA),
                false,
                now.minusSeconds(600),
                null,
                null
            ),
            new com.eunhyehymn.infrastructure.persistence.UserHymnStateEntity(
                new com.eunhyehymn.infrastructure.persistence.UserHymnStateId(userId, hymnB),
                false,
                now,
                null,
                null
            )
        ));

        String response = mockMvc.perform(get("/me/history")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode data = objectMapper.readTree(response).get("data");
        assertThat(data.get(0).get("id").asText()).isEqualTo(hymnB.toString());
        assertThat(data.get(1).get("id").asText()).isEqualTo(hymnA.toString());
    }
}
