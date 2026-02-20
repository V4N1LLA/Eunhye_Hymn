package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.RecommendHymnsUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.infrastructure.security.AuthRateLimitService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
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
    private final RecommendHymnsUseCase recommendHymnsUseCase;
    private final AuthRateLimitService authRateLimitService;

    public AiController(
        RecommendHymnsUseCase recommendHymnsUseCase,
        AuthRateLimitService authRateLimitService
    ) {
        this.recommendHymnsUseCase = recommendHymnsUseCase;
        this.authRateLimitService = authRateLimitService;
    }

    @PostMapping("/hymn-recommendations")
    public ApiResponse<HymnRecommendationResponse> recommend(
        @RequestBody @Validated HymnRecommendationRequest request,
        Authentication authentication
    ) {
        UUID userId = parseUserId(authentication);
        authRateLimitService.checkOrThrow("ai_recommend", userId.toString());

        try {
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

            return ApiResponse.success(new HymnRecommendationResponse(
                items,
                result.requestedMaxResults(),
                result.candidateCount()
            ));
        } catch (ApiException ex) {
            authRateLimitService.recordOutcome("ai_recommend", false);
            throw ex;
        }
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
