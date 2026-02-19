package com.eunhyehymn.application.usecases;

import com.eunhyehymn.application.ports.TokenHashService;
import com.eunhyehymn.application.ports.TokenService;
import com.eunhyehymn.domain.model.AuthIdentity;
import com.eunhyehymn.domain.model.RefreshToken;
import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserPasswordCredential;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.AuthIdentityRepository;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import com.eunhyehymn.domain.repository.RefreshTokenRepository;
import com.eunhyehymn.domain.repository.UserPasswordCredentialRepository;
import com.eunhyehymn.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class UserPasswordSignupUseCase {
    static final String PROVIDER_LOCAL_USER = "LOCAL_USER";
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9._-]{3,100}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AuthIdentityRepository authIdentityRepository;
    private final UserPasswordCredentialRepository userPasswordCredentialRepository;
    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final TokenHashService tokenHashService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenTtlSeconds;

    public UserPasswordSignupUseCase(
        AuthIdentityRepository authIdentityRepository,
        UserPasswordCredentialRepository userPasswordCredentialRepository,
        UserRepository userRepository,
        InviteCodeRepository inviteCodeRepository,
        RefreshTokenRepository refreshTokenRepository,
        TokenService tokenService,
        TokenHashService tokenHashService,
        PasswordEncoder passwordEncoder,
        long refreshTokenTtlSeconds
    ) {
        this.authIdentityRepository = authIdentityRepository;
        this.userPasswordCredentialRepository = userPasswordCredentialRepository;
        this.userRepository = userRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.tokenHashService = tokenHashService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    @Transactional
    public LoginResult signup(String loginId, String password, String inviteCode) {
        String rawLoginId = normalizeRawLoginId(loginId);
        String normalizedLoginId = normalizeLookupLoginId(rawLoginId);
        String normalizedInviteCode = normalizeInviteCode(inviteCode);
        validateCredentialFormat(normalizedLoginId, password);

        if (normalizedInviteCode == null || normalizedInviteCode.isBlank()) {
            throw new InvalidInviteCodeException("초대코드가 비어있습니다");
        }

        if (authIdentityRepository.findByProviderAndProviderSubject(PROVIDER_LOCAL_USER, normalizedLoginId).isPresent()) {
            throw new DuplicateLoginIdException("이미 사용 중인 로그인 ID입니다.");
        }

        inviteCodeRepository.findByCode(normalizedInviteCode)
            .orElseThrow(() -> new InvalidInviteCodeException("유효하지 않은 초대코드입니다"));

        boolean incremented = inviteCodeRepository.incrementUsedCount(normalizedInviteCode);
        if (!incremented) {
            throw new InvalidInviteCodeException("유효하지 않은 초대코드입니다");
        }

        Instant now = Instant.now();
        UUID userId = UUID.randomUUID();
        User user = new User(userId, rawLoginId, Role.USER, UserStatus.ACTIVE, now, now);
        userRepository.save(user);

        try {
            authIdentityRepository.save(new AuthIdentity(
                UUID.randomUUID(),
                userId,
                PROVIDER_LOCAL_USER,
                normalizedLoginId,
                null,
                now
            ));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateLoginIdException("이미 사용 중인 로그인 ID입니다.");
        }

        userPasswordCredentialRepository.save(new UserPasswordCredential(
            userId,
            passwordEncoder.encode(password),
            now,
            now
        ));

        String accessToken = tokenService.issueAccessToken(user.id().toString(), user.role().name());
        String refreshToken = tokenService.issueRefreshToken();
        String hash = tokenHashService.hash(refreshToken);
        refreshTokenRepository.save(new RefreshToken(
            UUID.randomUUID(),
            user.id(),
            hash,
            now.plusSeconds(refreshTokenTtlSeconds),
            null,
            now
        ));

        return new LoginResult(accessToken, refreshToken, true);
    }

    private void validateCredentialFormat(String loginId, String password) {
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new InvalidCredentialFormatException("로그인 ID는 영문/숫자/._- 조합 3-100자여야 합니다.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidCredentialFormatException("비밀번호는 최소 8자 이상이어야 합니다.");
        }
    }

    static String normalizeRawLoginId(String loginId) {
        return loginId == null ? "" : loginId.trim();
    }

    static String normalizeLookupLoginId(String loginId) {
        if (loginId == null) {
            return "";
        }
        return loginId.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null) {
            return null;
        }
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }

    public record LoginResult(String accessToken, String refreshToken, boolean newUser) {
    }

    public static class InvalidInviteCodeException extends RuntimeException {
        public InvalidInviteCodeException(String message) {
            super(message);
        }
    }

    public static class DuplicateLoginIdException extends RuntimeException {
        public DuplicateLoginIdException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialFormatException extends RuntimeException {
        public InvalidCredentialFormatException(String message) {
            super(message);
        }
    }
}
