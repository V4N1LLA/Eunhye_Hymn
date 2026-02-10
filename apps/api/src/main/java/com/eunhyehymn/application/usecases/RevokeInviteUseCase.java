package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.time.Instant;
import java.util.UUID;

public class RevokeInviteUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public RevokeInviteUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public InviteCode revoke(UUID id) {
        InviteCode invite = inviteCodeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("초대코드를 찾을 수 없습니다"));
        InviteCode revoked = new InviteCode(
            invite.id(), invite.code(), invite.maxUses(), invite.usedCount(),
            invite.expiresAt(), Instant.now(), invite.createdAt()
        );
        return inviteCodeRepository.save(revoked);
    }
}
