package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.UserHymnState;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import java.time.Instant;
import java.util.UUID;

public class ToggleFavoriteUseCase {
    private final UserHymnStateRepository userHymnStateRepository;

    public ToggleFavoriteUseCase(UserHymnStateRepository userHymnStateRepository) {
        this.userHymnStateRepository = userHymnStateRepository;
    }

    public UserHymnState toggle(UUID userId, UUID hymnId) {
        UserHymnState updated = userHymnStateRepository.findByUserIdAndHymnId(userId, hymnId)
            .map(existing -> new UserHymnState(
                existing.userId(),
                existing.hymnId(),
                !existing.favorite(),
                existing.lastOpenedAt(),
                existing.lastPartPlayed(),
                existing.lastPlayPositionMs()
            ))
            .orElseGet(() -> new UserHymnState(userId, hymnId, true, Instant.now(), null, null));

        return userHymnStateRepository.save(updated);
    }
}
