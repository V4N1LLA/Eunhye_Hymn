package com.eunhyehymn.presentation.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
        "security.rate-limit.auth.enabled=true",
        "security.rate-limit.auth.window-seconds=60",
        "security.rate-limit.auth.max-attempts-per-key=2"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRateLimitApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void loginEndpointReturnsTooManyRequestsAfterThreshold() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "loginId", "member.one",
            "password", "wrong-password"
        ));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("user_login_failed"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("user_login_failed"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("too_many_requests"));
    }

    @Test
    void spoofedForwardedIpHeaderDoesNotBypassRateLimitByDefault() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
            "loginId", "member.spoof",
            "password", "wrong-password"
        ));

        mockMvc.perform(post("/auth/login")
                .header("X-Forwarded-For", "203.0.113.10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("user_login_failed"));

        mockMvc.perform(post("/auth/login")
                .header("X-Forwarded-For", "203.0.113.11")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("user_login_failed"));

        mockMvc.perform(post("/auth/login")
                .header("X-Forwarded-For", "203.0.113.12")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("too_many_requests"));
    }
}
