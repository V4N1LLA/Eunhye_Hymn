package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.User;
import com.eunhyehymn.domain.model.UserStatus;
import com.eunhyehymn.domain.repository.UserRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.UserMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        UserEntity saved = userJpaRepository.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
            .map(UserMapper::toDomain)
            .toList();
    }

    @Override
    public long countByRoleAndStatus(Role role, UserStatus status) {
        return userJpaRepository.countByRoleAndStatus(role, status);
    }
}
