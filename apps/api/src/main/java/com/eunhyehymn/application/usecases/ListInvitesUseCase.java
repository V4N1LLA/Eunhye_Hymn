package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.InviteCode;
import com.eunhyehymn.domain.repository.InviteCodeRepository;
import java.util.List;

public class ListInvitesUseCase {
    private final InviteCodeRepository inviteCodeRepository;

    public ListInvitesUseCase(InviteCodeRepository inviteCodeRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
    }

    public List<InviteCode> listAll() {
        return inviteCodeRepository.findAll();
    }
}
