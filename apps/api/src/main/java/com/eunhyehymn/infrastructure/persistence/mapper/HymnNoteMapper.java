package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.HymnNote;
import com.eunhyehymn.infrastructure.persistence.HymnNoteEntity;

public final class HymnNoteMapper {
    private HymnNoteMapper() {
    }

    public static HymnNote toDomain(HymnNoteEntity entity) {
        return new HymnNote(
            entity.getId(),
            entity.getUserId(),
            entity.getHymnId(),
            entity.getContent(),
            entity.getUpdatedAt()
        );
    }

    public static HymnNoteEntity toEntity(HymnNote note) {
        return new HymnNoteEntity(
            note.id(),
            note.userId(),
            note.hymnId(),
            note.content(),
            note.updatedAt()
        );
    }
}
