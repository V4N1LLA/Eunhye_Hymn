package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CreateInviteUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public CreateInviteUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public InviteCode create(String code, Integer maxUses, Instant expiresAt) {
        if (inviteCodeRepository.findByCode(code).isPresent()) {
            throw new ApiException(
                HttpStatus.CONFLICT, "invite_code_duplicate", "이미 존재하는 초대코드입니다", null);
        }
        InviteCode inviteCode = new InviteCode(
            UUID.randomUUID(), code, maxUses, 0, expiresAt, null, Instant.now()
        );
        return inviteCodeRepository.save(inviteCode);
    }
}
