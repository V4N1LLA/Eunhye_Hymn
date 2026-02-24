package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.time.Instant;
import java.util.Locale;

public class ValidateInviteCodeUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public ValidateInviteCodeUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public boolean validate(String code) {
        String normalizedCode = normalizeInviteCode(code);
        return inviteCodeRepository.findByCode(normalizedCode)
            .map(this::isValid)
            .orElse(false);
    }

    private boolean isValid(InviteCode inviteCode) {
        if (!inviteCode.enabled()) {
            return false;
        }
        if (inviteCode.expiresAt() != null && inviteCode.expiresAt().isBefore(Instant.now())) {
            return false;
        }
        if (inviteCode.maxUses() != null && inviteCode.usedCount() >= inviteCode.maxUses()) {
            return false;
        }
        return true;
    }

    private String normalizeInviteCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
