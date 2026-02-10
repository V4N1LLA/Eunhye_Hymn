package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.InviteCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InviteCodeRepository {
    InviteCode save(InviteCode inviteCode);

    Optional<InviteCode> findByCode(String code);

    Optional<InviteCode> findById(UUID id);

    List<InviteCode> findAll();
}
