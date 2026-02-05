package com.eunhyehymn.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserHymnStateId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "hymn_id", nullable = false)
    private UUID hymnId;

    protected UserHymnStateId() {
    }

    public UserHymnStateId(UUID userId, UUID hymnId) {
        this.userId = userId;
        this.hymnId = hymnId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getHymnId() {
        return hymnId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserHymnStateId that = (UserHymnStateId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(hymnId, that.hymnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, hymnId);
    }
}
