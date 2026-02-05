package com.eunhyehymn.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunhyehymn.domain.model.Role;
import com.eunhyehymn.domain.model.UserStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FlywayRepositoryIntegrationTest {
    @Autowired
    private Flyway flyway;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void flywayAppliesInitAndRepositoryPersists() {
        // Why: confirm migrations and persistence wiring are usable in tests.
        boolean hasV2 = Arrays.stream(flyway.info().applied())
            .anyMatch(info -> "2".equals(info.getVersion().getVersion()));
        assertThat(hasV2).isTrue();

        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity(
            id,
            "테스트 사용자",
            Role.USER,
            UserStatus.ACTIVE,
            Instant.now(),
            null
        );

        userJpaRepository.save(entity);

        assertThat(userJpaRepository.findById(id)).isPresent();
    }
}
