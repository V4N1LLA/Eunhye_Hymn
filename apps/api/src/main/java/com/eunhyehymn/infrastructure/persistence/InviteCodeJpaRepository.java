package com.eunhyehymn.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InviteCodeJpaRepository extends JpaRepository<InviteCodeEntity, String> {

    @Modifying
    @Query("UPDATE InviteCodeEntity e SET e.usedCount = e.usedCount + 1 " +
           "WHERE e.code = :code AND e.enabled = true " +
           "AND (e.maxUses IS NULL OR e.usedCount < e.maxUses) " +
           "AND (e.expiresAt IS NULL OR e.expiresAt > CURRENT_TIMESTAMP)")
    int incrementUsedCount(@Param("code") String code);
}
