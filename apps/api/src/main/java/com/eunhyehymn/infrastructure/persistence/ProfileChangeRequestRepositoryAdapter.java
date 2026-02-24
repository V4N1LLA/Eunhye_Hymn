package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.ProfileChangeRequest;
import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import com.eunhyehymn.domain.repository.ProfileChangeRequestRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.ProfileChangeRequestMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileChangeRequestRepositoryAdapter implements ProfileChangeRequestRepository {
    private final ProfileChangeRequestJpaRepository jpaRepository;

    public ProfileChangeRequestRepositoryAdapter(ProfileChangeRequestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ProfileChangeRequest save(ProfileChangeRequest request) {
        ProfileChangeRequestEntity saved = jpaRepository.save(ProfileChangeRequestMapper.toEntity(request));
        return ProfileChangeRequestMapper.toDomain(saved);
    }

    @Override
    public Optional<ProfileChangeRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(ProfileChangeRequestMapper::toDomain);
    }

    @Override
    public Optional<ProfileChangeRequest> findPendingByUserId(UUID userId) {
        return jpaRepository
            .findTopByUserIdAndStatusOrderByRequestedAtDesc(userId, ProfileChangeRequestStatus.PENDING)
            .map(ProfileChangeRequestMapper::toDomain);
    }

    @Override
    public Optional<ProfileChangeRequest> findLatestByUserId(UUID userId) {
        return jpaRepository.findTopByUserIdOrderByRequestedAtDesc(userId).map(ProfileChangeRequestMapper::toDomain);
    }

    @Override
    public List<ProfileChangeRequest> findByStatus(ProfileChangeRequestStatus status) {
        return jpaRepository.findByStatusOrderByRequestedAtDesc(status).stream()
            .map(ProfileChangeRequestMapper::toDomain)
            .toList();
    }
}
