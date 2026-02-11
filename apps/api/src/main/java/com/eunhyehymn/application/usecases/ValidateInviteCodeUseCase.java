package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.time.Instant;

public class ValidateInviteCodeUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public ValidateInviteCodeUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public boolean validate(String code) {
        return inviteCodeRepository.findByCode(code)
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
}
