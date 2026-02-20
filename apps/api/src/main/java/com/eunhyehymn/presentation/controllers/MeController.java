package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.GetFavoriteUseCase;
import com.eunhyehymn.application.usecases.GetHistoryUseCase;
import com.eunhyehymn.application.usecases.GetHymnNoteUseCase;
import com.eunhyehymn.application.usecases.GetMyProfileUseCase;
import com.eunhyehymn.application.usecases.SaveHymnNoteUseCase;
import com.eunhyehymn.application.usecases.ToggleFavoriteUseCase;
import com.eunhyehymn.application.usecases.UpsertMyProfileUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@Validated
public class MeController {
    private final ToggleFavoriteUseCase toggleFavoriteUseCase;
    private final GetFavoriteUseCase getFavoriteUseCase;
    private final GetHymnNoteUseCase getHymnNoteUseCase;
    private final SaveHymnNoteUseCase saveHymnNoteUseCase;
    private final GetHistoryUseCase getHistoryUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpsertMyProfileUseCase upsertMyProfileUseCase;

    public MeController(
        ToggleFavoriteUseCase toggleFavoriteUseCase,
        GetFavoriteUseCase getFavoriteUseCase,
        GetHymnNoteUseCase getHymnNoteUseCase,
        SaveHymnNoteUseCase saveHymnNoteUseCase,
        GetHistoryUseCase getHistoryUseCase,
        GetMyProfileUseCase getMyProfileUseCase,
        UpsertMyProfileUseCase upsertMyProfileUseCase
    ) {
        this.toggleFavoriteUseCase = toggleFavoriteUseCase;
        this.getFavoriteUseCase = getFavoriteUseCase;
        this.getHymnNoteUseCase = getHymnNoteUseCase;
        this.saveHymnNoteUseCase = saveHymnNoteUseCase;
        this.getHistoryUseCase = getHistoryUseCase;
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.upsertMyProfileUseCase = upsertMyProfileUseCase;
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(Authentication authentication) {
        UUID userId = parseUserId(authentication);
        GetMyProfileUseCase.Result profile = getMyProfileUseCase.get(userId);
        return ApiResponse.success(toProfileResponse(profile));
    }

    @PutMapping("/profile")
    public ApiResponse<ProfileResponse> upsertProfile(
        @RequestBody @Validated UpsertProfileRequest request,
        Authentication authentication
    ) {
        UUID userId = parseUserId(authentication);
        upsertMyProfileUseCase.upsert(
            userId,
            request.churchName(),
            request.name(),
            request.group()
        );
        GetMyProfileUseCase.Result updated = getMyProfileUseCase.get(userId);
        return ApiResponse.success(toProfileResponse(updated));
    }

    @PostMapping("/favorites/{hymnId}")
    public ApiResponse<FavoriteResponse> toggleFavorite(@PathVariable UUID hymnId, Authentication authentication) {
        UUID userId = parseUserId(authentication);
        boolean favorite = toggleFavoriteUseCase.toggle(userId, hymnId).favorite();
        return ApiResponse.success(new FavoriteResponse(favorite));
    }

    @GetMapping("/favorites/{hymnId}")
    public ApiResponse<FavoriteResponse> getFavorite(@PathVariable UUID hymnId, Authentication authentication) {
        UUID userId = parseUserId(authentication);
        boolean favorite = getFavoriteUseCase.get(userId, hymnId);
        return ApiResponse.success(new FavoriteResponse(favorite));
    }

    @GetMapping("/hymns/{hymnId}/note")
    public ApiResponse<NoteResponse> getNote(@PathVariable UUID hymnId, Authentication authentication) {
        UUID userId = parseUserId(authentication);
        String content = getHymnNoteUseCase.get(userId, hymnId).map(note -> note.content()).orElse(null);
        return ApiResponse.success(new NoteResponse(content));
    }

    @PutMapping("/hymns/{hymnId}/note")
    public ApiResponse<NoteResponse> saveNote(
        @PathVariable UUID hymnId,
        @RequestBody @Validated NoteRequest request,
        Authentication authentication
    ) {
        UUID userId = parseUserId(authentication);
        String content = saveHymnNoteUseCase.save(userId, hymnId, request.content()).content();
        return ApiResponse.success(new NoteResponse(content));
    }

    @GetMapping("/history")
    public ApiResponse<java.util.List<HistoryItemResponse>> history(Authentication authentication) {
        UUID userId = parseUserId(authentication);
        var items = getHistoryUseCase.getHistory(userId).stream()
            .map(item -> new HistoryItemResponse(
                item.hymn().id(),
                item.hymn().title(),
                item.hymn().number(),
                item.hymn().tags(),
                item.lastOpenedAt()
            ))
            .toList();
        return ApiResponse.success(items);
    }

    private ProfileResponse toProfileResponse(GetMyProfileUseCase.Result profile) {
        return new ProfileResponse(
            profile.userId().toString(),
            profile.role().name(),
            profile.displayName(),
            profile.churchName(),
            profile.name(),
            profile.group(),
            profile.profileCompleted(),
            profile.profileUpdatedAt()
        );
    }

    private UUID parseUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "authentication is required", null);
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_principal", "invalid authenticated principal", null);
        }
    }

    public record FavoriteResponse(boolean favorite) {
    }

    public record ProfileResponse(
        String userId,
        String role,
        String displayName,
        String churchName,
        String name,
        String group,
        boolean profileCompleted,
        java.time.Instant profileUpdatedAt
    ) {
    }

    public record UpsertProfileRequest(
        @NotBlank String churchName,
        @NotBlank String name,
        @NotBlank String group
    ) {
    }

    public record NoteRequest(@NotBlank String content) {
    }

    public record NoteResponse(String content) {
    }

    public record HistoryItemResponse(
        UUID id,
        String title,
        String number,
        String tags,
        java.time.Instant lastOpenedAt
    ) {
    }
}
