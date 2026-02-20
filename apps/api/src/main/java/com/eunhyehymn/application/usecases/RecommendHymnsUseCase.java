package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.ExternalAiException;
import com.eunhyehymn.application.ports.HymnRecommendationClient;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

public class RecommendHymnsUseCase {
    private final HymnRepository hymnRepository;
    private final HymnRecommendationClient hymnRecommendationClient;
    private final int maxCandidateHymns;
    private final int defaultMaxResults;
    private final int maxSituationChars;

    public RecommendHymnsUseCase(
        HymnRepository hymnRepository,
        HymnRecommendationClient hymnRecommendationClient,
        int maxCandidateHymns,
        int defaultMaxResults,
        int maxSituationChars
    ) {
        this.hymnRepository = hymnRepository;
        this.hymnRecommendationClient = hymnRecommendationClient;
        this.maxCandidateHymns = Math.max(1, maxCandidateHymns);
        this.defaultMaxResults = clampResultCount(defaultMaxResults);
        this.maxSituationChars = Math.max(32, maxSituationChars);
    }

    public Result recommend(String rawSituation, Integer requestedMaxResults) {
        String situation = normalizeSituation(rawSituation);
        int maxResults = requestedMaxResults == null ? defaultMaxResults : clampResultCount(requestedMaxResults);

        List<Hymn> candidates = hymnRepository.findEnabled().stream()
            .limit(maxCandidateHymns)
            .toList();

        if (candidates.isEmpty()) {
            return new Result(List.of(), maxResults, 0);
        }

        Map<UUID, Hymn> hymnById = candidates.stream().collect(Collectors.toMap(Hymn::id, hymn -> hymn));
        List<HymnRecommendationClient.CandidateHymn> aiCandidates = candidates.stream()
            .map(hymn -> new HymnRecommendationClient.CandidateHymn(
                hymn.id(),
                sanitize(hymn.number(), 32),
                sanitize(hymn.title(), 80),
                sanitize(hymn.tags(), 120)
            ))
            .toList();

        final List<HymnRecommendationClient.Recommendation> aiRecommendations;
        try {
            aiRecommendations = hymnRecommendationClient.recommend(situation, aiCandidates, maxResults);
        } catch (ExternalAiException e) {
            throw new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ai_unavailable",
                "AI recommendation is temporarily unavailable.",
                null
            );
        }

        List<RecommendedHymn> items = toRecommendedHymns(aiRecommendations, hymnById, maxResults);
        if (items.isEmpty()) {
            items = fallbackRecommendations(candidates, maxResults);
        }
        return new Result(items, maxResults, candidates.size());
    }

    private String normalizeSituation(String rawSituation) {
        if (rawSituation == null || rawSituation.isBlank()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "Situation is required.",
                null
            );
        }
        return sanitize(rawSituation, maxSituationChars);
    }

    private int clampResultCount(int requested) {
        return Math.max(1, Math.min(5, requested));
    }

    private List<RecommendedHymn> toRecommendedHymns(
        List<HymnRecommendationClient.Recommendation> recommendations,
        Map<UUID, Hymn> hymnById,
        int maxResults
    ) {
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of();
        }

        List<RecommendedHymn> items = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (HymnRecommendationClient.Recommendation recommendation : recommendations) {
            if (recommendation == null || recommendation.hymnId() == null || !seen.add(recommendation.hymnId())) {
                continue;
            }
            Hymn hymn = hymnById.get(recommendation.hymnId());
            if (hymn == null) {
                continue;
            }
            items.add(new RecommendedHymn(
                hymn.id(),
                hymn.number(),
                hymn.title(),
                hymn.tags(),
                sanitize(recommendation.reason(), 140)
            ));
            if (items.size() >= maxResults) {
                break;
            }
        }
        return items;
    }

    private List<RecommendedHymn> fallbackRecommendations(List<Hymn> candidates, int maxResults) {
        List<RecommendedHymn> fallback = new ArrayList<>();
        for (Hymn hymn : candidates) {
            fallback.add(new RecommendedHymn(
                hymn.id(),
                hymn.number(),
                hymn.title(),
                hymn.tags(),
                "Recommended from available hymns."
            ));
            if (fallback.size() >= maxResults) {
                break;
            }
        }
        return fallback;
    }

    private String sanitize(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen);
    }

    public record RecommendedHymn(
        UUID id,
        String number,
        String title,
        String tags,
        String reason
    ) {
    }

    public record Result(
        List<RecommendedHymn> items,
        int requestedMaxResults,
        int candidateCount
    ) {
    }
}
