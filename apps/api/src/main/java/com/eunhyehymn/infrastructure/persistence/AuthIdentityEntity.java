package com.eunhyehymn.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "auth_identities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_auth_identity_provider_subject", columnNames = {"provider", "provider_subject"})
    }
)
public class AuthIdentityEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_subject", nullable = false)
    private String providerSubject;

    @Column
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthIdentityEntity() {
    }

    public AuthIdentityEntity(
        UUID id,
        UUID userId,
        String provider,
        String providerSubject,
        String email,
        Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
