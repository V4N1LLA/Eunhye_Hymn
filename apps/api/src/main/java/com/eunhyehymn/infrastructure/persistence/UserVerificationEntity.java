package com.eunhyehymn.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_verifications")
public class UserVerificationEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "invite_code", length = 50)
    private String inviteCode;

    @Column(name = "invite_verified_at")
    private Instant inviteVerifiedAt;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserVerificationEntity() {
    }

    public UserVerificationEntity(
        UUID userId,
        String inviteCode,
        Instant inviteVerifiedAt,
        String phoneNumber,
        Instant phoneVerifiedAt,
        Instant updatedAt
    ) {
        this.userId = userId;
        this.inviteCode = inviteCode;
        this.inviteVerifiedAt = inviteVerifiedAt;
        this.phoneNumber = phoneNumber;
        this.phoneVerifiedAt = phoneVerifiedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public Instant getInviteVerifiedAt() {
        return inviteVerifiedAt;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Instant getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
