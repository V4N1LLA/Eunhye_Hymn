package com.eunhyehymn.infrastructure.persistence;

import com.eunhyehymn.domain.model.ProfileChangeRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileChangeRequestJpaRepository extends JpaRepository<ProfileChangeRequestEntity, UUID> {
    Optional<ProfileChangeRequestEntity> findTopByUserIdAndStatusOrderByRequestedAtDesc(
        UUID userId,
        ProfileChangeRequestStatus status
    );

    Optional<ProfileChangeRequestEntity> findTopByUserIdOrderByRequestedAtDesc(UUID userId);

    List<ProfileChangeRequestEntity> findByStatusOrderByRequestedAtDesc(ProfileChangeRequestStatus status);
}
