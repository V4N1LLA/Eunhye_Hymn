package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class AuthFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void meEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/me/profile"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("unauthorized"));
    }

    @Test
    void adminEndpointReturnsForbiddenEnvelope() throws Exception {
        TokenPair tokens = devLogin();

        mockMvc.perform(get("/admin/test")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("forbidden"));
    }

    @Test
    void devLoginAllowsMeProfileAccess() throws Exception {
        TokenPair tokens = devLogin();

        mockMvc.perform(get("/me/profile")
                .header("Authorization", "Bearer " + tokens.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(tokens.userId.toString()))
            .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void refreshRotatesToken() throws Exception {
        TokenPair tokens = devLogin();

        String payload = objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()));
        String response = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode data = objectMapper.readTree(response).get("data");
        String newRefresh = data.get("refreshToken").asText();

        String oldPayload = objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refreshToken()));
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(oldPayload))
            .andExpect(status().isUnauthorized());

        String newPayload = objectMapper.writeValueAsString(Map.of("refreshToken", newRefresh));
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(newPayload))
            .andExpect(status().isOk());
    }

    private TokenPair devLogin() throws Exception {
        UUID userId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(Map.of(
            "userId", userId.toString(),
            "role", "USER",
            "displayName", "개발 사용자"
        ));

        String response = mockMvc.perform(post("/auth/dev/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode data = objectMapper.readTree(response).get("data");
        return new TokenPair(
            userId,
            data.get("accessToken").asText(),
            data.get("refreshToken").asText()
        );
    }

    private record TokenPair(UUID userId, String accessToken, String refreshToken) {
    }
}
