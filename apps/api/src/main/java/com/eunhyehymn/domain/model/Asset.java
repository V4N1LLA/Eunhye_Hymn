package com.eunhyehymn.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Asset(
    UUID id,
    UUID hymnId,
    AssetType type,
    PartType part,
    String url,
    String checksum,
    String version,
    Instant createdAt
) {
}
