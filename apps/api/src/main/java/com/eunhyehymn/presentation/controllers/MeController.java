package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.GetHistoryUseCase;
import com.eunhyehymn.application.usecases.GetHymnNoteUseCase;
import com.eunhyehymn.application.usecases.SaveHymnNoteUseCase;
import com.eunhyehymn.application.usecases.ToggleFavoriteUseCase;
import com.eunhyehymn.common.response.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/me")
@Validated
public class MeController {
    private final ToggleFavoriteUseCase toggleFavoriteUseCase;
    private final GetHymnNoteUseCase getHymnNoteUseCase;
    private final SaveHymnNoteUseCase saveHymnNoteUseCase;
    private final GetHistoryUseCase getHistoryUseCase;

    public MeController(
        ToggleFavoriteUseCase toggleFavoriteUseCase,
        GetHymnNoteUseCase getHymnNoteUseCase,
        SaveHymnNoteUseCase saveHymnNoteUseCase,
        GetHistoryUseCase getHistoryUseCase
    ) {
        this.toggleFavoriteUseCase = toggleFavoriteUseCase;
        this.getHymnNoteUseCase = getHymnNoteUseCase;
        this.saveHymnNoteUseCase = saveHymnNoteUseCase;
        this.getHistoryUseCase = getHistoryUseCase;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, String>> profile(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
            .findFirst()
            .map(authority -> authority.getAuthority().replace("ROLE_", ""))
            .orElse("UNKNOWN");
        return ApiResponse.success(Map.of("userId", authentication.getName(), "role", role));
    }

    @PostMapping("/favorites/{hymnId}")
    public ApiResponse<FavoriteResponse> toggleFavorite(@PathVariable UUID hymnId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        boolean favorite = toggleFavoriteUseCase.toggle(userId, hymnId).favorite();
        return ApiResponse.success(new FavoriteResponse(favorite));
    }

    @GetMapping("/hymns/{hymnId}/note")
    public ApiResponse<NoteResponse> getNote(@PathVariable UUID hymnId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String content = getHymnNoteUseCase.get(userId, hymnId).map(note -> note.content()).orElse(null);
        return ApiResponse.success(new NoteResponse(content));
    }

    @PutMapping("/hymns/{hymnId}/note")
    public ApiResponse<NoteResponse> saveNote(
        @PathVariable UUID hymnId,
        @RequestBody @Validated NoteRequest request,
        Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        String content = saveHymnNoteUseCase.save(userId, hymnId, request.content()).content();
        return ApiResponse.success(new NoteResponse(content));
    }

    @GetMapping("/history")
    public ApiResponse<java.util.List<HistoryItemResponse>> history(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
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

    public record FavoriteResponse(boolean favorite) {
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
