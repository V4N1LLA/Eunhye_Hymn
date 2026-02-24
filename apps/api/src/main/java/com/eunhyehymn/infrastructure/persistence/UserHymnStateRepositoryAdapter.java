package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.UserHymnState;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.UserHymnStateMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserHymnStateRepositoryAdapter implements UserHymnStateRepository {
    private final UserHymnStateJpaRepository userHymnStateJpaRepository;

    public UserHymnStateRepositoryAdapter(UserHymnStateJpaRepository userHymnStateJpaRepository) {
        this.userHymnStateJpaRepository = userHymnStateJpaRepository;
    }

    @Override
    public UserHymnState save(UserHymnState state) {
        UserHymnStateEntity saved = userHymnStateJpaRepository.save(UserHymnStateMapper.toEntity(state));
        return UserHymnStateMapper.toDomain(saved);
    }

    @Override
    public Optional<UserHymnState> findByUserIdAndHymnId(UUID userId, UUID hymnId) {
        return userHymnStateJpaRepository
            .findByIdUserIdAndIdHymnId(userId, hymnId)
            .map(UserHymnStateMapper::toDomain);
    }

    @Override
    public List<UserHymnState> findByUserIdOrderByLastOpenedAtDesc(UUID userId) {
        return userHymnStateJpaRepository.findByIdUserIdOrderByLastOpenedAtDesc(userId).stream()
            .map(UserHymnStateMapper::toDomain)
            .toList();
    }

    @Override
    public void deleteByHymnId(UUID hymnId) {
        userHymnStateJpaRepository.deleteByIdHymnId(hymnId);
    }
}
