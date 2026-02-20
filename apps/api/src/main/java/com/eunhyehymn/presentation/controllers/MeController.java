package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.GetFavoriteUseCase;
import com.eunhyehymn.application.usecases.GetHistoryUseCase;
import com.eunhyehymn.application.usecases.GetHymnNoteUseCase;
import com.eunhyehymn.application.usecases.GetLatestMyProfileChangeRequestUseCase;
import com.eunhyehymn.application.usecases.GetMyProfileUseCase;
import com.eunhyehymn.application.usecases.RequestMyProfileChangeUseCase;
import com.eunhyehymn.application.usecases.SaveHymnNoteUseCase;
import com.eunhyehymn.application.usecases.ToggleFavoriteUseCase;
import com.eunhyehymn.application.usecases.UpsertMyProfileUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
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
    private final RequestMyProfileChangeUseCase requestMyProfileChangeUseCase;
    private final GetLatestMyProfileChangeRequestUseCase getLatestMyProfileChangeRequestUseCase;
    private final UserVerificationRepository userVerificationRepository;

    public MeController(
        ToggleFavoriteUseCase toggleFavoriteUseCase,
        GetFavoriteUseCase getFavoriteUseCase,
        GetHymnNoteUseCase getHymnNoteUseCase,
        SaveHymnNoteUseCase saveHymnNoteUseCase,
        GetHistoryUseCase getHistoryUseCase,
        GetMyProfileUseCase getMyProfileUseCase,
        UpsertMyProfileUseCase upsertMyProfileUseCase,
        RequestMyProfileChangeUseCase requestMyProfileChangeUseCase,
        GetLatestMyProfileChangeRequestUseCase getLatestMyProfileChangeRequestUseCase,
        UserVerificationRepository userVerificationRepository
    ) {
        this.toggleFavoriteUseCase = toggleFavoriteUseCase;
        this.getFavoriteUseCase = getFavoriteUseCase;
        this.getHymnNoteUseCase = getHymnNoteUseCase;
        this.saveHymnNoteUseCase = saveHymnNoteUseCase;
        this.getHistoryUseCase = getHistoryUseCase;
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.upsertMyProfileUseCase = upsertMyProfileUseCase;
        this.requestMyProfileChangeUseCase = requestMyProfileChangeUseCase;
        this.getLatestMyProfileChangeRequestUseCase = getLatestMyProfileChangeRequestUseCase;
        this.userVerificationRepository = userVerificationRepository;
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(Authentication authentication) {
        UUID userId = parseUserId(authentication);
        GetMyProfileUseCase.Result profile = getMyProfileUseCase.get(userId);
        VerificationStatus verificationStatus = resolveVerificationStatus(profile.role(), userId);
        return ApiResponse.success(toProfileResponse(profile, verificationStatus));
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
            request.group(),
            request.gender()
        );
        GetMyProfileUseCase.Result updated = getMyProfileUseCase.get(userId);
        VerificationStatus verificationStatus = resolveVerificationStatus(updated.role(), userId);
        return ApiResponse.success(toProfileResponse(updated, verificationStatus));
    }

    @PostMapping("/profile-change-requests")
    public ApiResponse<ProfileChangeRequestResponse> requestProfileChange(
        @RequestBody @Validated ProfileChangeRequestRequest request,
        Authentication authentication
    ) {
        UUID userId = parseUserId(authentication);
        ProfileChangeRequest saved = requestMyProfileChangeUseCase.request(
            userId,
            request.churchName(),
            request.name(),
            request.group(),
            request.gender()
        );
        return ApiResponse.success(toProfileChangeRequestResponse(saved));
    }

    @GetMapping("/profile-change-requests/latest")
    public ApiResponse<ProfileChangeRequestResponse> latestProfileChangeRequest(Authentication authentication) {
        UUID userId = parseUserId(authentication);
        ProfileChangeRequest latest = getLatestMyProfileChangeRequestUseCase.get(userId);
        return ApiResponse.success(toProfileChangeRequestResponse(latest));
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

    private VerificationStatus resolveVerificationStatus(Role role, UUID userId) {
        if (role == Role.ADMIN) {
            return new VerificationStatus(true, true);
        }

        var verification = userVerificationRepository.findByUserId(userId).orElse(null);
        boolean inviteVerified = verification != null && verification.isInviteVerified();
        boolean phoneVerified = verification != null && verification.isPhoneVerified();
        return new VerificationStatus(inviteVerified, phoneVerified);
    }

    private ProfileResponse toProfileResponse(GetMyProfileUseCase.Result profile, VerificationStatus verificationStatus) {
        return new ProfileResponse(
            profile.userId().toString(),
            profile.role().name(),
            profile.displayName(),
            profile.churchName(),
            profile.name(),
            profile.group(),
            profile.gender(),
            profile.profileCompleted(),
            profile.profileUpdatedAt(),
            verificationStatus.inviteVerified(),
            verificationStatus.phoneVerified(),
            verificationStatus.inviteVerified() && verificationStatus.phoneVerified()
        );
    }

    private ProfileChangeRequestResponse toProfileChangeRequestResponse(ProfileChangeRequest request) {
        return new ProfileChangeRequestResponse(
            request.id().toString(),
            request.status().name(),
            request.churchName(),
            request.name(),
            request.groupName(),
            request.gender().name(),
            request.requestedAt(),
            request.reviewedBy() == null ? null : request.reviewedBy().toString(),
            request.reviewedAt(),
            request.rejectReason()
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
        String gender,
        boolean profileCompleted,
        java.time.Instant profileUpdatedAt,
        boolean inviteVerified,
        boolean phoneVerified,
        boolean verified
    ) {
    }

    public record UpsertProfileRequest(
        @NotBlank String churchName,
        @NotBlank String name,
        @NotBlank String group,
        String gender
    ) {
    }

    public record ProfileChangeRequestRequest(
        @NotBlank String churchName,
        @NotBlank String name,
        @NotBlank String group,
        String gender
    ) {
    }

    public record ProfileChangeRequestResponse(
        String id,
        String status,
        String churchName,
        String name,
        String group,
        String gender,
        java.time.Instant requestedAt,
        String reviewedBy,
        java.time.Instant reviewedAt,
        String rejectReason
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

    private record VerificationStatus(boolean inviteVerified, boolean phoneVerified) {
    }
}
