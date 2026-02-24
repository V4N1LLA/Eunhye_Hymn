package com.eunhyehymn.infrastructure.ai;

import com.eunhyehymn.application.ports.ExternalAiException;
import com.eunhyehymn.application.ports.HymnRecommendationClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GeminiHymnRecommendationClient implements HymnRecommendationClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration readTimeout;
    private final double temperature;
    private final int maxOutputTokens;

    public GeminiHymnRecommendationClient(
        ObjectMapper objectMapper,
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        int connectTimeoutSeconds,
        int readTimeoutSeconds,
        double temperature,
        int maxOutputTokens
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = model == null || model.isBlank() ? "gemini-2.5-flash-lite" : model.trim();
        this.readTimeout = Duration.ofSeconds(Math.max(1, readTimeoutSeconds));
        this.temperature = clampTemperature(temperature);
        this.maxOutputTokens = Math.max(64, maxOutputTokens);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
            .build();
    }

    @Override
    public List<Recommendation> recommend(String situation, List<CandidateHymn> candidates, int maxResults) {
        if (!enabled) {
            throw new ExternalAiException("Gemini AI is disabled.");
        }
        if (apiKey.isBlank()) {
            throw new ExternalAiException("Gemini API key is missing.");
        }
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        try {
            ObjectNode requestBody = buildRequestBody(situation, candidates, maxResults);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildEndpoint()))
                .header("Content-Type", "application/json")
                .timeout(readTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalAiException("Gemini API error: HTTP " + response.statusCode() + " " + extractError(response.body()));
            }
            return parseRecommendations(response.body(), maxResults);
        } catch (IOException e) {
            throw new ExternalAiException("Gemini API call failed.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalAiException("Gemini API call interrupted.", e);
        }
    }

    private ObjectNode buildRequestBody(String situation, List<CandidateHymn> candidates, int maxResults) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        ArrayNode userParts = userContent.putArray("parts");
        userParts.addObject().put("text", buildPrompt(situation, candidates, maxResults));

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("responseMimeType", "application/json");

        return body;
    }

    private String buildPrompt(String situation, List<CandidateHymn> candidates, int maxResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You recommend hymns for church members.\n");
        prompt.append("Choose up to ").append(Math.max(1, maxResults)).append(" hymns from catalog.\n");
        prompt.append("Return strict JSON only: ");
        prompt.append("{\"items\":[{\"id\":\"<uuid>\",\"reason\":\"<short reason>\"}]}\n");
        prompt.append("Do not include markdown, comments, or extra keys.\n");
        prompt.append("Use only IDs from catalog.\n");
        prompt.append("Catalog:\n");
        for (CandidateHymn candidate : candidates) {
            prompt
                .append(candidate.id())
                .append(" | ")
                .append(safe(candidate.number()))
                .append(" | ")
                .append(safe(candidate.title()))
                .append(" | ")
                .append(safe(candidate.tags()))
                .append('\n');
        }
        prompt.append("Situation: ").append(safe(situation)).append('\n');
        return prompt.toString();
    }

    private List<Recommendation> parseRecommendations(String body, int maxResults) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode candidatesNode = root.path("candidates");
        if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
            throw new ExternalAiException("Gemini returned no candidates.");
        }

        String rawText = collectCandidateText(candidatesNode.get(0));
        if (rawText.isBlank()) {
            throw new ExternalAiException("Gemini returned empty content.");
        }

        String jsonText = extractJson(rawText);
        JsonNode payload = objectMapper.readTree(jsonText);
        JsonNode itemsNode = payload.path("items");
        if (!itemsNode.isArray()) {
            return List.of();
        }

        List<Recommendation> recommendations = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (JsonNode itemNode : itemsNode) {
            String idText = itemNode.path("id").asText(null);
            if (idText == null || idText.isBlank()) {
                continue;
            }
            try {
                UUID hymnId = UUID.fromString(idText.trim());
                if (!seen.add(hymnId)) {
                    continue;
                }
                recommendations.add(new Recommendation(
                    hymnId,
                    truncate(itemNode.path("reason").asText(""), 140)
                ));
                if (recommendations.size() >= Math.max(1, maxResults)) {
                    break;
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore non-UUID ids and continue parsing the remaining items.
            }
        }
        return recommendations;
    }

    private String collectCandidateText(JsonNode candidateNode) {
        JsonNode parts = candidateNode.path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            String chunk = part.path("text").asText("");
            if (!chunk.isBlank()) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(chunk);
            }
        }
        return text.toString().trim();
    }

    private String extractJson(String generated) {
        String trimmed = generated.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String extractError(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = root.path("error").path("message").asText("");
            if (!message.isBlank()) {
                return truncate(message, 240);
            }
        } catch (IOException ignored) {
            // Fallback to body text if JSON parsing fails.
        }
        return truncate(body == null ? "" : body, 240);
    }

    private String buildEndpoint() {
        return baseUrl
            + "/v1beta/models/"
            + URLEncoder.encode(model, StandardCharsets.UTF_8)
            + ":generateContent?key="
            + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "https://generativelanguage.googleapis.com";
        }
        String normalized = url.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private double clampTemperature(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String normalized = safe(value);
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen);
    }
}
