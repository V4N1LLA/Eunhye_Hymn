package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import org.springframework.http.HttpStatus;

public class AdminRevokeInviteCodeUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public AdminRevokeInviteCodeUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public void revoke(String code) {
        InviteCode existing = inviteCodeRepository.findByCode(code)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "invite_code_not_found", "초대코드를 찾을 수 없습니다", null));

        inviteCodeRepository.save(existing.withEnabled(false));
    }
}
