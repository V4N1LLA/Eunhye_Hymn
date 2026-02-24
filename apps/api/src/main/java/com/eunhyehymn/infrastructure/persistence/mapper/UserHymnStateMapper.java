package com.eunhyehymn.infrastructure.persistence.mapper;

import com.eunhyehymn.domain.model.UserHymnState;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateEntity;
import com.eunhyehymn.infrastructure.persistence.UserHymnStateId;

public final class UserHymnStateMapper {
    private UserHymnStateMapper() {
    }

    public static UserHymnState toDomain(UserHymnStateEntity entity) {
        return new UserHymnState(
            entity.getId().getUserId(),
            entity.getId().getHymnId(),
            entity.isFavorite(),
            entity.getLastOpenedAt(),
            entity.getLastPartPlayed(),
            entity.getLastPlayPositionMs()
        );
    }

    public static UserHymnStateEntity toEntity(UserHymnState state) {
        return new UserHymnStateEntity(
            new UserHymnStateId(state.userId(), state.hymnId()),
            state.favorite(),
            state.lastOpenedAt(),
            state.lastPartPlayed(),
            state.lastPlayPositionMs()
        );
    }
}
