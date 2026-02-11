package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.AdminListUsersUseCase;
import com.eunhyehymn.application.usecases.AdminUpdateUserUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@Validated
public class AdminUserController {
    private final AdminListUsersUseCase adminListUsersUseCase;
    private final AdminUpdateUserUseCase adminUpdateUserUseCase;

    public AdminUserController(
        AdminListUsersUseCase adminListUsersUseCase,
        AdminUpdateUserUseCase adminUpdateUserUseCase
    ) {
        this.adminListUsersUseCase = adminListUsersUseCase;
        this.adminUpdateUserUseCase = adminUpdateUserUseCase;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        List<UserResponse> items = adminListUsersUseCase.listAll().stream()
            .map(UserResponse::from)
            .toList();
        return ApiResponse.success(items);
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> update(
        @PathVariable UUID id,
        @RequestBody UpdateUserRequest request,
        Authentication authentication
    ) {
        UUID requesterId = UUID.fromString(authentication.getName());
        Role role = parseEnum(Role.class, request.role(), "role");
        UserStatus status = parseEnum(UserStatus.class, request.status(), "status");
        User user = adminUpdateUserUseCase.update(requesterId, id, role, status);
        return ApiResponse.success(UserResponse.from(user));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validation_error",
                fieldName + " 값이 올바르지 않습니다: " + value, null);
        }
    }

    public record UserResponse(
        UUID id,
        String displayName,
        String role,
        String status,
        Instant createdAt,
        Instant lastLoginAt
    ) {
        static UserResponse from(User user) {
            return new UserResponse(
                user.id(),
                user.displayName(),
                user.role().name(),
                user.status().name(),
                user.createdAt(),
                user.lastLoginAt()
            );
        }
    }

    public record UpdateUserRequest(
        String role,
        String status
    ) {
    }
}
