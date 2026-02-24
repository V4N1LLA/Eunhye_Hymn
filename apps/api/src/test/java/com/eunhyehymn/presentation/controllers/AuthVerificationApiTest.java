package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eunhyehymn.application.ports.SmsSender;
import com.eunhyehymn.application.ports.SocialTokenVerifier;
import com.eunhyehymn.application.ports.SocialUserInfo;
import com.eunhyehymn.infrastructure.persistence.AssetJpaRepository;
import com.eunhyehymn.infrastructure.persistence.AuthIdentityJpaRepository;
import com.eunhyehymn.infrastructure.persistence.EventJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnJpaRepository;
import com.eunhyehymn.infrastructure.persistence.HymnNoteJpaRepository;
import com.eunhyehymn.infrastructure.persistence.InviteCodeEntity;
import com.eunhyehymn.infrastructure.persistence.InviteCodeJpaRepository;
import com.eunhyehymn.infrastructure.persistence.RefreshTokenJpaRepository;
import com.eunhyehymn.infrastructure.persistence.SmsVerificationRequestJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserJpaRepository;
import com.eunhyehymn.infrastructure.persistence.UserVerificationJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
class AuthVerificationApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private HymnJpaRepository hymnJpaRepository;
    @Autowired private AssetJpaRepository assetJpaRepository;
    @Autowired private HymnNoteJpaRepository hymnNoteJpaRepository;
    @Autowired private UserHymnStateJpaRepository userHymnStateJpaRepository;
    @Autowired private EventJpaRepository eventJpaRepository;
    @Autowired private AuthIdentityJpaRepository authIdentityJpaRepository;
    @Autowired private RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired private InviteCodeJpaRepository inviteCodeJpaRepository;
    @Autowired private UserVerificationJpaRepository userVerificationJpaRepository;
    @Autowired private SmsVerificationRequestJpaRepository smsVerificationRequestJpaRepository;

    @MockBean private SocialTokenVerifier socialTokenVerifier;
    @MockBean private SmsSender smsSender;

    @BeforeEach
    void setUp() {
        smsVerificationRequestJpaRepository.deleteAll();
        userVerificationJpaRepository.deleteAll();
        inviteCodeJpaRepository.deleteAll();
        eventJpaRepository.deleteAll();
        userHymnStateJpaRepository.deleteAll();
        hymnNoteJpaRepository.deleteAll();
        assetJpaRepository.deleteAll();
        authIdentityJpaRepository.deleteAll();
        refreshTokenJpaRepository.deleteAll();
        hymnJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        reset(smsSender);
    }

    @Test
    void inviteSmsVerificationFlowCompletesAndMarksProfileVerified() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "FLOW-CODE", null, "flow", null, 0, true, null, Instant.now()
        ));
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("kakao-token")))
            .thenReturn(new SocialUserInfo("flow-subject", "flow@example.com", "Flow User"));

        String token = socialLogin("kakao-token");

        mockMvc.perform(post("/auth/invite/validate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", "flow-code"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(true));

        String requestResponse = mockMvc.perform(post("/auth/sms/request")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phoneNumber", "010-1234-5678"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationId").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(smsSender).sendVerificationCode(anyString(), codeCaptor.capture());
        String capturedCode = codeCaptor.getValue();

        JsonNode requestData = objectMapper.readTree(requestResponse).get("data");
        String verificationId = requestData.get("verificationId").asText();

        mockMvc.perform(post("/auth/sms/verify")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "verificationId", verificationId,
                    "code", capturedCode
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verified").value(true))
            .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.inviteVerified").value(true))
            .andExpect(jsonPath("$.data.phoneVerified").value(true))
            .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    void withdrawDisablesAccountAndRevokesSession() throws Exception {
        inviteCodeJpaRepository.save(new InviteCodeEntity(
            "WITHDRAW-CODE", null, "withdraw", null, 0, true, null, Instant.now()
        ));
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("withdraw-token")))
            .thenReturn(new SocialUserInfo("withdraw-subject", "withdraw@example.com", "Withdraw User"));

        String token = socialLogin("withdraw-token");

        mockMvc.perform(post("/auth/invite/validate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", "WITHDRAW-CODE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(true));

        mockMvc.perform(post("/auth/withdraw")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        assertThat(refreshTokenJpaRepository.findAll())
            .allMatch(refreshToken -> refreshToken.getRevokedAt() != null);

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void smsRequestRequiresInviteVerificationFirst() throws Exception {
        when(socialTokenVerifier.verify(eq("KAKAO"), eq("plain-token")))
            .thenReturn(new SocialUserInfo("plain-subject", "plain@example.com", "Plain User"));

        String token = socialLogin("plain-token");

        mockMvc.perform(post("/auth/sms/request")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phoneNumber", "01012345678"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("invite_not_verified"));
    }

    private String socialLogin(String token) throws Exception {
        String response = mockMvc.perform(post("/auth/social")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "provider", "KAKAO",
                    "token", token
                ))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode data = objectMapper.readTree(response).get("data");
        return data.get("accessToken").asText();
    }
}
