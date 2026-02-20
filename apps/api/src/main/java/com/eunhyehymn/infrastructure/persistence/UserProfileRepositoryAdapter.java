package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.UserProfile;
import com.eunhyehymn.domain.repository.UserProfileRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.UserProfileMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserProfileRepositoryAdapter implements UserProfileRepository {
    private final UserProfileJpaRepository userProfileJpaRepository;

    public UserProfileRepositoryAdapter(UserProfileJpaRepository userProfileJpaRepository) {
        this.userProfileJpaRepository = userProfileJpaRepository;
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserProfileEntity saved = userProfileJpaRepository.save(UserProfileMapper.toEntity(userProfile));
        return UserProfileMapper.toDomain(saved);
    }

    @Override
    public Optional<UserProfile> findByUserId(UUID userId) {
        return userProfileJpaRepository.findById(userId).map(UserProfileMapper::toDomain);
    }
}
