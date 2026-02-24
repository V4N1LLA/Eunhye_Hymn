package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.eunhyehymn.application.ports.ExternalAiException;
import com.eunhyehymn.application.ports.HymnRecommendationClient;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiRecommendationApiTest {
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

    @Autowired
    private HymnNoteJpaRepository hymnNoteJpaRepository;

    @Autowired
    private EventJpaRepository eventJpaRepository;

    @Autowired
    private AuthIdentityJpaRepository authIdentityJpaRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenJpaRepository;

    @Autowired
    private InviteCodeJpaRepository inviteCodeJpaRepository;

    @MockBean
    private HymnRecommendationClient hymnRecommendationClient;

    private String accessToken;

    @BeforeEach
    void setUp() {
        inviteCodeJpaRepository.deleteAll();
        eventJpaRepository.deleteAll();
        hymnNoteJpaRepository.deleteAll();
        userHymnStateJpaRepository.deleteAll();
        assetJpaRepository.deleteAll();
        authIdentityJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, "tester", Role.USER, UserStatus.ACTIVE, Instant.now(), Instant.now());
        userJpaRepository.save(user);
        accessToken = jwtService.issueAccessToken(userId.toString(), Role.USER.name());
    }

    @Test
    void recommendRequiresAuthentication() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("situation", "Need a calm worship song"));

        mockMvc.perform(post("/ai/hymn-recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recommendReturnsFilteredAiResult() throws Exception {
        UUID enabledId = UUID.randomUUID();
        UUID disabledId = UUID.randomUUID();
        hymnJpaRepository.saveAll(List.of(
            new HymnEntity(enabledId, "Grace Song", "101", "grace,comfort", true, Instant.now()),
            new HymnEntity(disabledId, "Hidden Song", "102", "unused", false, Instant.now())
        ));

        when(hymnRecommendationClient.recommend(
            anyString(),
            org.mockito.ArgumentMatchers.<HymnRecommendationClient.CandidateHymn>anyList(),
            eq(2)
        )).thenReturn(List.of(
            new HymnRecommendationClient.Recommendation(disabledId, "Should be ignored"),
            new HymnRecommendationClient.Recommendation(enabledId, "Fits a quiet prayer time")
        ));

        String payload = objectMapper.writeValueAsString(Map.of(
            "situation", "For early morning prayer",
            "maxResults", 2
        ));

        mockMvc.perform(post("/ai/hymn-recommendations")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value(enabledId.toString()))
            .andExpect(jsonPath("$.data.items[0].title").value("Grace Song"));
    }

    @Test
    void recommendReturnsServiceUnavailableWhenAiFails() throws Exception {
        UUID hymnId = UUID.randomUUID();
        hymnJpaRepository.save(new HymnEntity(hymnId, "Hope Song", "201", "hope", true, Instant.now()));

        when(hymnRecommendationClient.recommend(
            anyString(),
            org.mockito.ArgumentMatchers.<HymnRecommendationClient.CandidateHymn>anyList(),
            anyInt()
        )).thenThrow(new ExternalAiException("temporary upstream failure"));

        String payload = objectMapper.writeValueAsString(Map.of("situation", "Need hopeful song"));

        mockMvc.perform(post("/ai/hymn-recommendations")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error.code").value("ai_unavailable"));
    }
}
