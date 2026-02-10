package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.time.Instant;
import java.util.Optional;

public class ValidateInviteUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public ValidateInviteUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public Result validate(String code) {
        Optional<InviteCode> found = inviteCodeRepository.findByCode(code);
        if (found.isEmpty()) {
            return new Result(false, null);
        }
        InviteCode invite = found.get();
        if (!invite.isValid()) {
            return new Result(false, invite.expiresAt());
        }
        return new Result(true, invite.expiresAt());
    }

    public record Result(boolean valid, Instant expiresAt) {
    }
}
