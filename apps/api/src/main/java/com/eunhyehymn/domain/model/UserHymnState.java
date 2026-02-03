package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record UserHymnState(
    UUID userId,
    UUID hymnId,
    boolean favorite,
    Instant lastOpenedAt,
    PartType lastPartPlayed,
    Long lastPlayPositionMs
) {
}
