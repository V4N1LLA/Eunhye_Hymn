package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.infrastructure.persistence.HymnEntity;

public final class HymnMapper {
    private HymnMapper() {
    }

    public static Hymn toDomain(HymnEntity entity) {
        return new Hymn(
            entity.getId(),
            entity.getTitle(),
            entity.getNumber(),
            entity.getTags(),
            entity.isEnabled(),
            entity.getCreatedAt()
        );
    }

    public static HymnEntity toEntity(Hymn hymn) {
        return new HymnEntity(
            hymn.id(),
            hymn.title(),
            hymn.number(),
            hymn.tags(),
            hymn.enabled(),
            hymn.createdAt()
        );
    }
}
