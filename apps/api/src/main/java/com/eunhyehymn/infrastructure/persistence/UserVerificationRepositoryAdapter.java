package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.UserVerification;
import com.eunhyehymn.domain.repository.UserVerificationRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.UserVerificationMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserVerificationRepositoryAdapter implements UserVerificationRepository {
    private final UserVerificationJpaRepository userVerificationJpaRepository;

    public UserVerificationRepositoryAdapter(UserVerificationJpaRepository userVerificationJpaRepository) {
        this.userVerificationJpaRepository = userVerificationJpaRepository;
    }

    @Override
    public Optional<UserVerification> findByUserId(UUID userId) {
        return userVerificationJpaRepository.findById(userId).map(UserVerificationMapper::toDomain);
    }

    @Override
    public UserVerification save(UserVerification verification) {
        UserVerificationEntity saved = userVerificationJpaRepository.save(UserVerificationMapper.toEntity(verification));
        return UserVerificationMapper.toDomain(saved);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        if (userVerificationJpaRepository.existsById(userId)) {
            userVerificationJpaRepository.deleteById(userId);
        }
    }
}
