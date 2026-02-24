package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.AssetType;
import com.eunhyehymn.domain.model.PartType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "assets",
    indexes = {
        @Index(name = "idx_assets_hymn_id", columnList = "hymn_id")
    }
)
public class AssetEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "hymn_id", nullable = false)
    private UUID hymnId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartType part;

    @Column(nullable = false)
    private String url;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column
    private String checksum;

    @Column
    private String version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssetEntity() {
    }

    public AssetEntity(
        UUID id,
        UUID hymnId,
        AssetType type,
        PartType part,
        String url,
        String objectKey,
        String checksum,
        String version,
        Instant createdAt
    ) {
        this.id = id;
        this.hymnId = hymnId;
        this.type = type;
        this.part = part;
        this.url = url;
        this.objectKey = objectKey;
        this.checksum = checksum;
        this.version = version;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getHymnId() {
        return hymnId;
    }

    public AssetType getType() {
        return type;
    }

    public PartType getPart() {
        return part;
    }

    public String getUrl() {
        return url;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
