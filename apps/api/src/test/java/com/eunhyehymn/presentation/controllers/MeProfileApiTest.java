package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnNoteJpaRepository;
import com.eunhyehymn.infrastructure.persistence.InviteCodeJpaRepository;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenEntity;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserEntity;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserProfileJpaRepository;
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
class MeProfileApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private UserProfileJpaRepository userProfileJpaRepository;
    @Autowired private UserHymnStateJpaRepository userHymnStateJpaRepository;
    @Autowired private HymnNoteJpaRepository hymnNoteJpaRepository;
    @Autowired private HymnJpaRepository hymnJpaRepository;
    @Autowired private EventJpaRepository eventJpaRepository;
    @Autowired private AuthIdentityJpaRepository authIdentityJpaRepository;
    @Autowired private RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired private InviteCodeJpaRepository inviteCodeJpaRepository;

    private UUID userId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        inviteCodeJpaRepository.deleteAll();
        eventJpaRepository.deleteAll();
        userHymnStateJpaRepository.deleteAll();
        hymnNoteJpaRepository.deleteAll();
        authIdentityJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userProfileJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        userId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
            userId,
            "프로필테스트",
            Role.USER,
            UserStatus.ACTIVE,
            Instant.now(),
            Instant.now()
        ));
        accessToken = jwtService.issueAccessToken(userId.toString(), Role.USER.name());
    }

    @Test
    void meProfileReturnsIncompleteWhenNoProfileExists() throws Exception {
        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(userId.toString()))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.profileCompleted").value(false))
            .andExpect(jsonPath("$.data.churchName").isEmpty())
            .andExpect(jsonPath("$.data.name").isEmpty())
            .andExpect(jsonPath("$.data.group").isEmpty());
    }

    @Test
    void upsertProfilePersistsAndReturnsCompleteProfile() throws Exception {
        mockMvc.perform(put("/me/profile")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "churchName", "은혜교회",
                    "name", "홍길동",
                    "group", "청년A"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profileCompleted").value(true))
            .andExpect(jsonPath("$.data.churchName").value("은혜교회"))
            .andExpect(jsonPath("$.data.name").value("홍길동"))
            .andExpect(jsonPath("$.data.group").value("청년A"))
            .andExpect(jsonPath("$.data.profileUpdatedAt").isNotEmpty());

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.profileCompleted").value(true))
            .andExpect(jsonPath("$.data.churchName").value("은혜교회"))
            .andExpect(jsonPath("$.data.name").value("홍길동"))
            .andExpect(jsonPath("$.data.group").value("청년A"));
    }

    @Test
    void withdrawDisablesAccountAndRevokesRefreshTokens() throws Exception {
        UUID refreshTokenId = UUID.randomUUID();
        refreshTokenJpaRepository.save(new RefreshTokenEntity(
            refreshTokenId,
            userId,
            "hash-1",
            Instant.now().plusSeconds(3600),
            null,
            Instant.now()
        ));

        mockMvc.perform(delete("/me/account")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        UserEntity withdrawn = userJpaRepository.findById(userId).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.DISABLED);

        RefreshTokenEntity token = refreshTokenJpaRepository.findById(refreshTokenId).orElseThrow();
        assertThat(token.getRevokedAt()).isNotNull();

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }
}
