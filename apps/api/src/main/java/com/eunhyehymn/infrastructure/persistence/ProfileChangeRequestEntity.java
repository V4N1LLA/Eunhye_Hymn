package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.Gender;
import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_change_requests")
public class ProfileChangeRequestEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "church_name", nullable = false)
    private String churchName;

    @Column(name = "member_name", nullable = false)
    private String memberName;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileChangeRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reject_reason")
    private String rejectReason;

    protected ProfileChangeRequestEntity() {
    }

    public ProfileChangeRequestEntity(
        UUID id,
        UUID userId,
        String churchName,
        String memberName,
        String groupName,
        Gender gender,
        ProfileChangeRequestStatus status,
        Instant requestedAt,
        UUID reviewedBy,
        Instant reviewedAt,
        String rejectReason
    ) {
        this.id = id;
        this.userId = userId;
        this.churchName = churchName;
        this.memberName = memberName;
        this.groupName = groupName;
        this.gender = gender;
        this.status = status;
        this.requestedAt = requestedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.rejectReason = rejectReason;
    }

    public UUID getId() {
        return id;
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

    public Gender getGender() {
        return gender;
    }

    public ProfileChangeRequestStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }
}
