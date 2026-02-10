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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
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
    public ApiResponse<UserResponse> update(@PathVariable UUID id, @RequestBody UpdateRequest request) {
        Role role = null;
        UserStatus status = null;
        if (request.role() != null) {
            try {
                role = Role.valueOf(request.role());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_role", "유효하지 않은 역할입니다", null);
            }
        }
        if (request.status() != null) {
            try {
                status = UserStatus.valueOf(request.status());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_status", "유효하지 않은 상태입니다", null);
            }
        }
        User updated = adminUpdateUserUseCase.update(id, role, status);
        return ApiResponse.success(UserResponse.from(updated));
    }

    public record UpdateRequest(String role, String status) {
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
                user.id(), user.displayName(), user.role().name(), user.status().name(),
                user.createdAt(), user.lastLoginAt()
            );
        }
    }
}
