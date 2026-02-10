package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.time.Instant;
import java.util.UUID;

public class CreateInviteUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public CreateInviteUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public InviteCode create(String code, Integer maxUses, Instant expiresAt) {
        InviteCode inviteCode = new InviteCode(
            UUID.randomUUID(), code, maxUses, 0, expiresAt, null, Instant.now()
        );
        return inviteCodeRepository.save(inviteCode);
    }
}
