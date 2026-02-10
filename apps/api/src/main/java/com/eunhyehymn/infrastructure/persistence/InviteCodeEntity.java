package com.eunhyehymn.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_codes")
public class InviteCodeEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InviteCodeEntity() {
    }

    public InviteCodeEntity(UUID id, String code, Integer maxUses, int usedCount,
                            Instant expiresAt, Instant revokedAt, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.maxUses = maxUses;
        this.usedCount = usedCount;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public Integer getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
