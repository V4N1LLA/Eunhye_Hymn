package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.AdminPasswordCredential;
import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.domain.repository.AdminPasswordCredentialRepository;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class UpdateAdminPasswordCredentialUseCase {
    private static final int MIN_LOGIN_ID_LENGTH = 3;
    private static final int MAX_LOGIN_ID_LENGTH = 100;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AdminPasswordCredentialRepository adminPasswordCredentialRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredLoginId;
    private final String configuredPassword;

    public UpdateAdminPasswordCredentialUseCase(
        AdminPasswordCredentialRepository adminPasswordCredentialRepository,
        AuthIdentityRepository authIdentityRepository,
        PasswordEncoder passwordEncoder,
        String configuredLoginId,
        String configuredPassword
    ) {
        this.adminPasswordCredentialRepository = adminPasswordCredentialRepository;
        this.authIdentityRepository = authIdentityRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredLoginId = configuredLoginId != null ? configuredLoginId.trim() : "";
        this.configuredPassword = configuredPassword != null ? configuredPassword : "";
    }

    @Transactional
    public Result update(UUID requesterId, String currentPassword, String newLoginId, String newPassword) {
        EffectiveCredential activeCredential = resolveEffectiveCredential();
        validateNewCredential(newLoginId, newPassword);

        String current = currentPassword != null ? currentPassword : "";
        if (!matchesCurrentPassword(activeCredential, current)) {
            throw new InvalidCredentialsException("Current admin password is incorrect.");
        }

        String normalizedLoginId = newLoginId.trim();
        String encodedPassword = passwordEncoder.encode(newPassword);
        Instant now = Instant.now();

        Optional<AdminPasswordCredential> storedCredential = adminPasswordCredentialRepository.find();
        if (storedCredential.isPresent()) {
            AdminPasswordCredential existing = storedCredential.get();
            adminPasswordCredentialRepository.save(new AdminPasswordCredential(
                existing.id(),
                normalizedLoginId,
                encodedPassword,
                existing.createdAt(),
                now
            ));
        } else {
            adminPasswordCredentialRepository.save(new AdminPasswordCredential(
                UUID.randomUUID(),
                normalizedLoginId,
                encodedPassword,
                now,
                now
            ));
        }

        syncLocalAdminIdentity(requesterId, normalizedLoginId, now);

        return new Result(normalizedLoginId, now);
    }

    private void syncLocalAdminIdentity(UUID requesterId, String loginId, Instant now) {
        Optional<AuthIdentity> identity = authIdentityRepository.findByUserIdAndProvider(
            requesterId,
            AdminPasswordLoginUseCase.PROVIDER_LOCAL_ADMIN
        );

        if (identity.isPresent()) {
            AuthIdentity existing = identity.get();
            if (!existing.providerSubject().equals(loginId)) {
                authIdentityRepository.save(new AuthIdentity(
                    existing.id(),
                    existing.userId(),
                    existing.provider(),
                    loginId,
                    existing.email(),
                    existing.createdAt()
                ));
            }
            return;
        }

        authIdentityRepository.save(new AuthIdentity(
            UUID.randomUUID(),
            requesterId,
            AdminPasswordLoginUseCase.PROVIDER_LOCAL_ADMIN,
            loginId,
            null,
            now
        ));
    }

    private EffectiveCredential resolveEffectiveCredential() {
        Optional<AdminPasswordCredential> storedCredential = adminPasswordCredentialRepository.find();
        if (storedCredential.isPresent()) {
            AdminPasswordCredential credential = storedCredential.get();
            return EffectiveCredential.fromStored(credential.loginId(), credential.passwordHash());
        }

        if (configuredLoginId.isBlank() || configuredPassword.isBlank()) {
            throw new LoginDisabledException("Admin ID/password login is not configured.");
        }
        return EffectiveCredential.fromConfigured(configuredLoginId, configuredPassword);
    }

    private boolean matchesCurrentPassword(EffectiveCredential credential, String currentPassword) {
        if (credential.fromConfigured()) {
            return secureEquals(credential.secret(), currentPassword);
        }
        return passwordEncoder.matches(currentPassword, credential.secret());
    }

    private static void validateNewCredential(String loginId, String password) {
        String normalizedLoginId = loginId != null ? loginId.trim() : "";
        String normalizedPassword = password != null ? password : "";

        if (normalizedLoginId.length() < MIN_LOGIN_ID_LENGTH || normalizedLoginId.length() > MAX_LOGIN_ID_LENGTH) {
            throw new InvalidCredentialFormatException("New login ID must be 3-100 characters.");
        }
        if (normalizedPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidCredentialFormatException("New password must be at least 8 characters.");
        }
    }

    private static boolean secureEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private record EffectiveCredential(String loginId, String secret, boolean fromConfigured) {
        static EffectiveCredential fromConfigured(String loginId, String password) {
            return new EffectiveCredential(loginId, password, true);
        }

        static EffectiveCredential fromStored(String loginId, String passwordHash) {
            return new EffectiveCredential(loginId, passwordHash, false);
        }
    }

    public record Result(String loginId, Instant updatedAt) {
    }

    public static class LoginDisabledException extends RuntimeException {
        public LoginDisabledException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialFormatException extends RuntimeException {
        public InvalidCredentialFormatException(String message) {
            super(message);
        }
    }
}
