package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminCreateInviteCodeUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public AdminCreateInviteCodeUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public InviteCode create(String code, UUID createdBy, String description, Integer maxUses, Instant expiresAt) {
        inviteCodeRepository.findByCode(code).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "invite_code_exists", "이미 존재하는 초대코드입니다: " + code, null);
        });

        InviteCode inviteCode = new InviteCode(
            code, createdBy, description, maxUses, 0, true, expiresAt, Instant.now()
        );
        return inviteCodeRepository.save(inviteCode);
    }
}
