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
    name = "hymn_notes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_hymn_notes_user_hymn", columnNames = {"user_id", "hymn_id"})
    }
)
public class HymnNoteEntity {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "hymn_id", nullable = false)
    private UUID hymnId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HymnNoteEntity() {
    }

    public HymnNoteEntity(UUID id, UUID userId, UUID hymnId, String content, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.hymnId = hymnId;
        this.content = content;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getHymnId() {
        return hymnId;
    }

    public String getContent() {
        return content;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
