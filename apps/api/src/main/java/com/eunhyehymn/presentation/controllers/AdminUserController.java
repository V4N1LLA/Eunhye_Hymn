package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.AdminCreateUserUseCase;
import com.eunhyehymn.application.usecases.AdminDeleteUserUseCase;
import com.eunhyehymn.application.usecases.AdminListUsersUseCase;
import com.eunhyehymn.application.usecases.AdminUpdateUserUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserProfile;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.model.UserVerification;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.domain.repository.UserProfileRepository;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@Validated
public class AdminUserController {
    private final AdminCreateUserUseCase adminCreateUserUseCase;
    private final AdminDeleteUserUseCase adminDeleteUserUseCase;
    private final AdminListUsersUseCase adminListUsersUseCase;
    private final AdminUpdateUserUseCase adminUpdateUserUseCase;
    private final AuthIdentityRepository authIdentityRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;

    public AdminUserController(
        AdminCreateUserUseCase adminCreateUserUseCase,
        AdminDeleteUserUseCase adminDeleteUserUseCase,
        AdminListUsersUseCase adminListUsersUseCase,
        AdminUpdateUserUseCase adminUpdateUserUseCase,
        AuthIdentityRepository authIdentityRepository,
        UserProfileRepository userProfileRepository,
        UserVerificationRepository userVerificationRepository
    ) {
        this.adminCreateUserUseCase = adminCreateUserUseCase;
        this.adminDeleteUserUseCase = adminDeleteUserUseCase;
        this.adminListUsersUseCase = adminListUsersUseCase;
        this.adminUpdateUserUseCase = adminUpdateUserUseCase;
        this.authIdentityRepository = authIdentityRepository;
        this.userProfileRepository = userProfileRepository;
        this.userVerificationRepository = userVerificationRepository;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(@RequestParam(required = false) String churchName) {
        String churchFilter = normalizeNullable(churchName);

        List<User> users = adminListUsersUseCase.listAll();
        List<UUID> userIds = users.stream().map(User::id).toList();

        Map<UUID, List<AuthIdentity>> identitiesByUserId = loadIdentitiesByUserId(userIds);
        Map<UUID, UserProfile> profilesByUserId = loadProfilesByUserId(userIds);
        Map<UUID, UserVerification> verificationsByUserId = loadVerificationsByUserId(userIds);

        if (churchFilter != null) {
            users = users.stream()
                .filter(user -> isChurchMatched(profilesByUserId.get(user.id()), churchFilter))
                .toList();
        }

        List<UserResponse> items = users.stream()
            .map(user -> toUserResponse(
                user,
                identitiesByUserId.getOrDefault(user.id(), List.of()),
                profilesByUserId.get(user.id()),
                verificationsByUserId.get(user.id())
            ))
            .toList();
        return ApiResponse.success(items);
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@RequestBody @Validated CreateUserRequest request) {
        Role role = parseEnum(Role.class, request.role(), "role");
        UserStatus status = parseEnum(UserStatus.class, request.status(), "status");
        User user = adminCreateUserUseCase.create(request.displayName(), role, status);
        return ApiResponse.success(loadUserResponse(user));
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(
        @PathVariable UUID id,
        @RequestBody UpdateUserRequest request,
        Authentication authentication
    ) {
        UUID requesterId = parseRequesterId(authentication);
        Role role = parseEnum(Role.class, request.role(), "role");
        UserStatus status = parseEnum(UserStatus.class, request.status(), "status");
        User user = adminUpdateUserUseCase.update(
            requesterId,
            id,
            role,
            status,
            request.displayName(),
            request.churchName(),
            request.name(),
            request.group(),
            request.gender(),
            request.phoneNumber()
        );
        return ApiResponse.success(loadUserResponse(user));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<UserResponse> delete(@PathVariable UUID id, Authentication authentication) {
        UUID requesterId = parseRequesterId(authentication);
        User user = adminDeleteUserUseCase.delete(requesterId, id);
        return ApiResponse.success(loadUserResponse(user));
    }

    private UserResponse loadUserResponse(User user) {
        List<AuthIdentity> identities = loadIdentitiesByUserId(List.of(user.id())).getOrDefault(user.id(), List.of());
        UserProfile profile = userProfileRepository.findByUserId(user.id()).orElse(null);
        UserVerification verification = userVerificationRepository.findByUserId(user.id()).orElse(null);
        return toUserResponse(user, identities, profile, verification);
    }

    private Map<UUID, List<AuthIdentity>> loadIdentitiesByUserId(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return authIdentityRepository.findByUserIdIn(userIds).stream()
            .collect(Collectors.groupingBy(AuthIdentity::userId));
    }

    private Map<UUID, UserProfile> loadProfilesByUserId(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findByUserIdIn(userIds).stream()
            .collect(Collectors.toMap(
                UserProfile::userId,
                profile -> profile,
                (left, right) -> right
            ));
    }

    private Map<UUID, UserVerification> loadVerificationsByUserId(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userVerificationRepository.findByUserIdIn(userIds).stream()
            .collect(Collectors.toMap(
                UserVerification::userId,
                verification -> verification,
                (left, right) -> right
            ));
    }

    private UserResponse toUserResponse(
        User user,
        List<AuthIdentity> identities,
        UserProfile profile,
        UserVerification verification
    ) {
        List<AuthIdentity> sortedIdentities = identities.stream()
            .sorted(Comparator.comparing(AuthIdentity::createdAt).reversed())
            .toList();

        List<AuthIdentitySummaryResponse> identityResponses = sortedIdentities.stream()
            .map(identity -> new AuthIdentitySummaryResponse(
                identity.provider(),
                maskProviderSubject(identity.providerSubject()),
                normalizeNullable(identity.email()),
                maskEmail(identity.email()),
                identity.createdAt()
            ))
            .toList();

        String primaryEmail = sortedIdentities.stream()
            .map(AuthIdentity::email)
            .map(this::normalizeNullable)
            .filter(email -> email != null && !email.isBlank())
            .findFirst()
            .orElse(null);

        UserProfileResponse profileResponse = profile == null
            ? null
            : new UserProfileResponse(
                profile.churchName(),
                profile.name(),
                profile.groupName(),
                profile.gender() == null ? null : profile.gender().name(),
                profile.updatedAt()
            );

        UserVerificationResponse verificationResponse = verification == null
            ? null
            : new UserVerificationResponse(
                verification.phoneNumber(),
                verification.phoneVerifiedAt(),
                verification.isPhoneVerified(),
                verification.updatedAt()
            );

        return new UserResponse(
            user.id(),
            user.displayName(),
            user.role().name(),
            user.status().name(),
            user.createdAt(),
            user.lastLoginAt(),
            primaryEmail,
            profileResponse,
            verificationResponse,
            identityResponses
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isChurchMatched(UserProfile profile, String filter) {
        if (profile == null || profile.churchName() == null) {
            return false;
        }
        String church = profile.churchName().trim();
        return !church.isEmpty() && church.equalsIgnoreCase(filter);
    }

    private String maskProviderSubject(String providerSubject) {
        if (providerSubject == null || providerSubject.isBlank()) {
            return null;
        }

        String value = providerSubject.trim();
        if (value.length() <= 6) {
            return value.charAt(0) + "***";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 2);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String normalized = email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return normalized.length() <= 4
                ? normalized.charAt(0) + "***"
                : normalized.substring(0, 2) + "***" + normalized.substring(normalized.length() - 1);
        }

        String local = normalized.substring(0, atIndex);
        String domain = normalized.substring(atIndex + 1);
        String maskedLocal;
        if (local.length() <= 1) {
            maskedLocal = local.charAt(0) + "***";
        } else if (local.length() == 2) {
            maskedLocal = local.charAt(0) + "*";
        } else {
            maskedLocal = local.substring(0, 1) + "***" + local.substring(local.length() - 1);
        }
        return maskedLocal + "@" + domain;
    }

    private UUID parseRequesterId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized", "authentication is required", null);
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_principal", "invalid authenticated principal", null);
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(enumClass, normalizedValue);
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "invalid " + fieldName + " value: " + value,
                null
            );
        }
    }

    public record CreateUserRequest(
        @NotBlank String displayName,
        String role,
        String status
    ) {
    }

    public record UserResponse(
        UUID id,
        String displayName,
        String role,
        String status,
        Instant createdAt,
        Instant lastLoginAt,
        String primaryEmail,
        UserProfileResponse profile,
        UserVerificationResponse verification,
        List<AuthIdentitySummaryResponse> identities
    ) {
    }

    public record UserProfileResponse(
        String churchName,
        String name,
        String group,
        String gender,
        Instant updatedAt
    ) {
    }

    public record UserVerificationResponse(
        String phoneNumber,
        Instant phoneVerifiedAt,
        boolean phoneVerified,
        Instant updatedAt
    ) {
    }

    public record AuthIdentitySummaryResponse(
        String provider,
        String providerSubjectMasked,
        String email,
        String emailMasked,
        Instant createdAt
    ) {
        public AuthIdentitySummaryResponse {
            provider = provider == null ? null : provider.toUpperCase(Locale.ROOT);
        }
    }

    public record UpdateUserRequest(
        String role,
        String status,
        String displayName,
        String churchName,
        String name,
        String group,
        String gender,
        String phoneNumber
    ) {
    }
}
