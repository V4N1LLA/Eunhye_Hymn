package com.eunhyehymn.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "church_name", nullable = false)
    private String churchName;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfileEntity() {
    }

    public UserProfileEntity(UUID userId, String churchName, String memberName, String groupName, Instant updatedAt) {
        this.userId = userId;
        this.churchName = churchName;
        this.memberName = memberName;
        this.groupName = groupName;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getChurchName() {
        return churchName;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getGroupName() {
        return groupName;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
