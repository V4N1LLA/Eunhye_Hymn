package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Gender;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserProfile;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.model.UserVerification;
import com.eunhyehymn.domain.repository.UserProfileRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

public class AdminUpdateUserUseCase {
    private static final int MAX_DISPLAY_NAME_LENGTH = 64;
    private static final int MAX_PROFILE_TEXT_LENGTH = 255;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserVerificationRepository userVerificationRepository;

    public AdminUpdateUserUseCase(
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        UserVerificationRepository userVerificationRepository
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userVerificationRepository = userVerificationRepository;
    }

    @Transactional
    public User update(
        UUID requesterId,
        UUID targetUserId,
        Role role,
        UserStatus status,
        String displayName,
        String churchName,
        String name,
        String group,
        String gender,
        String phoneNumber
    ) {
        boolean accountUpdateRequested = role != null || status != null || displayName != null;
        boolean profileUpdateRequested = churchName != null || name != null || group != null || gender != null;
        boolean phoneUpdateRequested = phoneNumber != null;

        if (!accountUpdateRequested && !profileUpdateRequested && !phoneUpdateRequested) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "empty_update",
                "at least one field is required",
                null
            );
        }

        User existing = userRepository.findById(targetUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "user not found", null));

        if (requesterId.equals(targetUserId) && role != null && role != existing.role()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "self_role_change",
                "cannot change your own role",
                null
            );
        }

        Role effectiveRole = role != null ? role : existing.role();
        UserStatus effectiveStatus = status != null ? status : existing.status();
        String effectiveDisplayName = displayName != null
            ? normalizeDisplayName(displayName)
            : existing.displayName();

        boolean wouldLoseAdmin = existing.role() == Role.ADMIN
            && (effectiveRole != Role.ADMIN || effectiveStatus != UserStatus.ACTIVE);

        if (wouldLoseAdmin) {
            long activeAdminCount = userRepository.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "last_admin",
                    "cannot update the last active admin",
                    null
                );
            }
        }

        User effectiveUser = existing;
        if (effectiveRole != existing.role()
            || effectiveStatus != existing.status()
            || !effectiveDisplayName.equals(existing.displayName())) {
            User updated = new User(
                existing.id(),
                effectiveDisplayName,
                effectiveRole,
                effectiveStatus,
                existing.createdAt(),
                existing.lastLoginAt()
            );
            effectiveUser = userRepository.save(updated);
        }

        if (profileUpdateRequested) {
            upsertUserProfile(targetUserId, churchName, name, group, gender);
        }
        if (phoneUpdateRequested) {
            upsertUserVerification(targetUserId, phoneNumber);
        }

        return effectiveUser;
    }

    private void upsertUserProfile(
        UUID userId,
        String churchName,
        String name,
        String group,
        String gender
    ) {
        UserProfile existingProfile = userProfileRepository.findByUserId(userId).orElse(null);
        String nextChurchName = resolveProfileText(churchName, existingProfile == null ? null : existingProfile.churchName(), "churchName");
        String nextName = resolveProfileText(name, existingProfile == null ? null : existingProfile.name(), "name");
        String nextGroup = resolveProfileText(group, existingProfile == null ? null : existingProfile.groupName(), "group");
        Gender nextGender = resolveGender(gender, existingProfile == null ? Gender.UNKNOWN : existingProfile.gender());

        UserProfile nextProfile = new UserProfile(
            userId,
            nextChurchName,
            nextName,
            nextGroup,
            nextGender,
            Instant.now()
        );
        userProfileRepository.save(nextProfile);
    }

    private void upsertUserVerification(UUID userId, String rawPhoneNumber) {
        UserVerification existingVerification = userVerificationRepository.findByUserId(userId).orElse(null);
        String nextPhoneNumber = normalizeOptionalPhoneNumber(rawPhoneNumber);
        Instant now = Instant.now();

        Instant nextPhoneVerifiedAt = null;
        if (nextPhoneNumber != null) {
            boolean unchangedPhone = existingVerification != null
                && nextPhoneNumber.equals(existingVerification.phoneNumber());
            nextPhoneVerifiedAt = unchangedPhone
                ? existingVerification.phoneVerifiedAt()
                : now;
        }

        UserVerification nextVerification = new UserVerification(
            userId,
            existingVerification == null ? null : existingVerification.inviteCode(),
            existingVerification == null ? null : existingVerification.inviteVerifiedAt(),
            nextPhoneNumber,
            nextPhoneVerifiedAt,
            now
        );
        userVerificationRepository.save(nextVerification);
    }

    private String normalizeDisplayName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "displayName is required",
                null
            );
        }
        if (normalized.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "displayName is too long",
                null
            );
        }
        return normalized;
    }

    private String resolveProfileText(String input, String existingValue, String fieldName) {
        if (input == null) {
            if (existingValue != null && !existingValue.isBlank()) {
                return existingValue;
            }
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                fieldName + " is required",
                null
            );
        }

        String normalized = input.trim();
        if (normalized.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                fieldName + " is required",
                null
            );
        }
        if (normalized.length() > MAX_PROFILE_TEXT_LENGTH) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                fieldName + " is too long",
                null
            );
        }
        return normalized;
    }

    private Gender resolveGender(String input, Gender existingGender) {
        if (input == null || input.trim().isEmpty()) {
            return existingGender == null ? Gender.UNKNOWN : existingGender;
        }
        try {
            return Gender.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "invalid gender",
                null
            );
        }
    }

    private String normalizeOptionalPhoneNumber(String rawPhoneNumber) {
        String normalized = rawPhoneNumber == null ? "" : rawPhoneNumber.replaceAll("\\D", "");
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() < 10 || normalized.length() > 11) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                "phoneNumber must be 10-11 digits",
                null
            );
        }
        return normalized;
    }
}
