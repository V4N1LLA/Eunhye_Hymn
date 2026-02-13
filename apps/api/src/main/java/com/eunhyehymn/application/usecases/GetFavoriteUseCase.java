package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import java.util.UUID;

public class GetFavoriteUseCase {
    private final UserHymnStateRepository userHymnStateRepository;

    public GetFavoriteUseCase(UserHymnStateRepository userHymnStateRepository) {
        this.userHymnStateRepository = userHymnStateRepository;
    }

    public boolean get(UUID userId, UUID hymnId) {
        return userHymnStateRepository.findByUserIdAndHymnId(userId, hymnId)
            .map(state -> state.favorite())
            .orElse(false);
    }
}
