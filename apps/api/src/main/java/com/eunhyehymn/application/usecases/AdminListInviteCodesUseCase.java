package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.util.List;

public class AdminListInviteCodesUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public AdminListInviteCodesUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public List<InviteCode> listAll() {
        return inviteCodeRepository.findAll();
    }
}
