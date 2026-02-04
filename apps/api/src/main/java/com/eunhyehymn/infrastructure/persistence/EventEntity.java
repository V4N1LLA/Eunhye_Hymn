package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.EventType;
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
    name = "events",
    indexes = {
        @Index(name = "idx_events_user_created", columnList = "user_id, created_at")
    }
)
public class EventEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "hymn_id")
    private UUID hymnId;

    @Enumerated(EnumType.STRING)
    @Column
    private PartType part;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EventEntity() {
    }

    public EventEntity(
        UUID id,
        UUID userId,
        EventType eventType,
        UUID hymnId,
        PartType part,
        String metadataJson,
        Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.eventType = eventType;
        this.hymnId = hymnId;
        this.part = part;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public UUID getHymnId() {
        return hymnId;
    }

    public PartType getPart() {
        return part;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
