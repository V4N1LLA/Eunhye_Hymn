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
    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column
    private String description;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InviteCodeEntity() {
    }

    public InviteCodeEntity(String code, UUID createdBy, String description, Integer maxUses,
                            int usedCount, boolean enabled, Instant expiresAt, Instant createdAt) {
        this.code = code;
        this.createdBy = createdBy;
        this.description = description;
        this.maxUses = maxUses;
        this.usedCount = usedCount;
        this.enabled = enabled;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getCode() { return code; }
    public UUID getCreatedBy() { return createdBy; }
    public String getDescription() { return description; }
    public Integer getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }
    public boolean isEnabled() { return enabled; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
