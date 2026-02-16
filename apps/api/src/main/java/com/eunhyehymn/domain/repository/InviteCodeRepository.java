package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.InviteCode;
import java.util.List;
import java.util.Optional;

public interface InviteCodeRepository {
    InviteCode save(InviteCode inviteCode);

    Optional<InviteCode> findByCode(String code);

    List<InviteCode> findAll();

    /**
     * 초대코드의 usedCount를 원자적으로 1 증가시킵니다.
     * maxUses가 null이거나 usedCount < maxUses인 경우에만 증가합니다.
     *
     * @return 증가 성공 시 true, 한도 초과 시 false
     */
    boolean incrementUsedCount(String code);
}
