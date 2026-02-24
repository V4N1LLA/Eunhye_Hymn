package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.SmsVerificationRequest;
import com.eunhyehymn.domain.repository.SmsVerificationRequestRepository;
import com.eunhyehymn.infrastructure.persistence.mapper.SmsVerificationRequestMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SmsVerificationRequestRepositoryAdapter implements SmsVerificationRequestRepository {
    private final SmsVerificationRequestJpaRepository smsVerificationRequestJpaRepository;

    public SmsVerificationRequestRepositoryAdapter(SmsVerificationRequestJpaRepository smsVerificationRequestJpaRepository) {
        this.smsVerificationRequestJpaRepository = smsVerificationRequestJpaRepository;
    }

    @Override
    public SmsVerificationRequest save(SmsVerificationRequest request) {
        SmsVerificationRequestEntity saved = smsVerificationRequestJpaRepository.save(
            SmsVerificationRequestMapper.toEntity(request)
        );
        return SmsVerificationRequestMapper.toDomain(saved);
    }

    @Override
    public Optional<SmsVerificationRequest> findById(UUID id) {
        return smsVerificationRequestJpaRepository.findById(id).map(SmsVerificationRequestMapper::toDomain);
    }

    @Override
    public Optional<SmsVerificationRequest> findLatestByUserIdAndPhoneNumber(UUID userId, String phoneNumber) {
        return smsVerificationRequestJpaRepository.findFirstByUserIdAndPhoneNumberOrderByCreatedAtDesc(userId, phoneNumber)
            .map(SmsVerificationRequestMapper::toDomain);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        smsVerificationRequestJpaRepository.deleteByUserId(userId);
    }
}
