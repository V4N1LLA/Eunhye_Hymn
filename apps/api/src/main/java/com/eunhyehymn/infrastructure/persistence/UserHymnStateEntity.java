package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.PartType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "user_hymn_state",
    indexes = {
        @Index(name = "idx_user_hymn_state_user_opened", columnList = "user_id, last_opened_at")
    }
)
public class UserHymnStateEntity {
    @EmbeddedId
    private UserHymnStateId id;

    @Column(nullable = false)
    private boolean favorite;

    @Column(name = "last_opened_at")
    private Instant lastOpenedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_part_played")
    private PartType lastPartPlayed;

    @Column(name = "last_play_position_ms")
    private Long lastPlayPositionMs;

    protected UserHymnStateEntity() {
    }

    public UserHymnStateEntity(
        UserHymnStateId id,
        boolean favorite,
        Instant lastOpenedAt,
        PartType lastPartPlayed,
        Long lastPlayPositionMs
    ) {
        this.id = id;
        this.favorite = favorite;
        this.lastOpenedAt = lastOpenedAt;
        this.lastPartPlayed = lastPartPlayed;
        this.lastPlayPositionMs = lastPlayPositionMs;
    }

    public UserHymnStateId getId() {
        return id;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public Instant getLastOpenedAt() {
        return lastOpenedAt;
    }

    public PartType getLastPartPlayed() {
        return lastPartPlayed;
    }

    public Long getLastPlayPositionMs() {
        return lastPlayPositionMs;
    }
}
