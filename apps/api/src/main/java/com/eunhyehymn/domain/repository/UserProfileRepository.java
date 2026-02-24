package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.UserProfile;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {
    UserProfile save(UserProfile userProfile);

    Optional<UserProfile> findByUserId(UUID userId);
}
