package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.InviteCode;
import java.util.List;
import java.util.Optional;

public interface InviteCodeRepository {
    InviteCode save(InviteCode inviteCode);

    Optional<InviteCode> findByCode(String code);

    List<InviteCode> findAll();
}
