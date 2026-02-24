package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.RecommendHymnsUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.infrastructure.security.AuthRateLimitService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@Validated
public class AiController {
    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final RecommendHymnsUseCase recommendHymnsUseCase;
    private final AuthRateLimitService authRateLimitService;
    private final MeterRegistry meterRegistry;

    public AiController(
        RecommendHymnsUseCase recommendHymnsUseCase,
        AuthRateLimitService authRateLimitService,
        MeterRegistry meterRegistry
    ) {
        this.recommendHymnsUseCase = recommendHymnsUseCase;
        this.authRateLimitService = authRateLimitService;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/hymn-recommendations")
    public ApiResponse<HymnRecommendationResponse> recommend(
        @RequestBody @Validated HymnRecommendationRequest request,
        Authentication authentication
    ) {
        long startedNanos = System.nanoTime();
        String resultTag = "success";
        int responseItemCount = 0;
        int candidateCount = 0;
        boolean fallbackUsed = false;

        try {
            UUID userId = parseUserId(authentication);
            authRateLimitService.checkOrThrow("ai_recommend", userId.toString());

            RecommendHymnsUseCase.Result result = recommendHymnsUseCase.recommend(
                request.situation(),
                request.maxResults()
            );
            authRateLimitService.recordOutcome("ai_recommend", true);
            List<HymnRecommendationItem> items = result.items().stream()
                .map(item -> new HymnRecommendationItem(
                    item.id(),
                    item.number(),
                    item.title(),
                    item.tags(),
                    item.reason()
                ))
                .toList();
            responseItemCount = items.size();
            candidateCount = result.candidateCount();
            fallbackUsed = result.fallbackUsed();

            return ApiResponse.success(new HymnRecommendationResponse(
                items,
                result.requestedMaxResults(),
                result.candidateCount()
            ));
        } catch (ApiException ex) {
            resultTag = normalizeResultTag(ex.getCode());
            authRateLimitService.recordOutcome("ai_recommend", false);
            throw ex;
        } catch (RuntimeException ex) {
            resultTag = "unexpected_error";
            authRateLimitService.recordOutcome("ai_recommend", false);
            throw ex;
        } finally {
            long elapsedNanos = System.nanoTime() - startedNanos;
            meterRegistry.counter("ai_recommend_requests_total", "result", resultTag).increment();
            meterRegistry.timer("ai_recommend_latency_seconds", "result", resultTag).record(elapsedNanos, TimeUnit.NANOSECONDS);
            meterRegistry.summary("ai_recommend_candidate_count", "result", resultTag).record(candidateCount);
            meterRegistry.summary("ai_recommend_response_items", "result", resultTag).record(responseItemCount);
            if (fallbackUsed) {
                meterRegistry.counter("ai_recommend_fallback_total", "result", resultTag).increment();
            }

            if ("success".equals(resultTag)) {
                log.info(
                    "ai recommendation completed: result={} elapsed_ms={} fallbackUsed={} candidateCount={} responseItems={}",
                    resultTag,
                    TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                    fallbackUsed,
                    candidateCount,
                    responseItemCount
                );
            } else {
                log.warn(
                    "ai recommendation failed: result={} elapsed_ms={} fallbackUsed={} candidateCount={} responseItems={}",
                    resultTag,
                    TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                    fallbackUsed,
                    candidateCount,
                    responseItemCount
                );
            }
        }
    }

    private String normalizeResultTag(String code) {
        if (code == null || code.isBlank()) {
            return "error";
        }
        return switch (code) {
            case "validation_error", "too_many_requests", "ai_unavailable", "unauthorized" -> code;
            default -> "error";
        };
    }

    private UUID parseUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication required.", null);
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (RuntimeException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "Invalid authentication subject.", null);
        }
    }

    public record HymnRecommendationRequest(
        @NotBlank String situation,
        @Min(1) @Max(5) Integer maxResults
    ) {
    }

    public record HymnRecommendationItem(
        UUID id,
        String number,
        String title,
        String tags,
        String reason
    ) {
    }

    public record HymnRecommendationResponse(
        List<HymnRecommendationItem> items,
        int requestedMaxResults,
        int candidateCount
    ) {
    }
}
