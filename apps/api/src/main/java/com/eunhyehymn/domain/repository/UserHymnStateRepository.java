package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.UserHymnState;
import java.util.Optional;
import java.util.UUID;

public interface UserHymnStateRepository {
    UserHymnState save(UserHymnState state);

    Optional<UserHymnState> findByUserIdAndHymnId(UUID userId, UUID hymnId);
}
