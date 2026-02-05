package com.eunhyehymn.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.model.UserHymnState;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GetHistoryUseCaseTest {
    @Test
    void historyUsesBatchLookupOnce() {
        UUID userId = UUID.randomUUID();
        UUID hymnA = UUID.randomUUID();
        UUID hymnB = UUID.randomUUID();

        UserHymnStateRepository stateRepository = new UserHymnStateRepository() {
            @Override
            public UserHymnState save(UserHymnState state) {
                return state;
            }

            @Override
            public Optional<UserHymnState> findByUserIdAndHymnId(UUID userId, UUID hymnId) {
                return Optional.empty();
            }

            @Override
            public List<UserHymnState> findByUserIdOrderByLastOpenedAtDesc(UUID userId) {
                return List.of(
                    new UserHymnState(userId, hymnB, false, Instant.now(), null, null),
                    new UserHymnState(userId, hymnA, false, Instant.now().minusSeconds(60), null, null)
                );
            }
        };

        AtomicInteger batchCalls = new AtomicInteger();
        HymnRepository hymnRepository = new HymnRepository() {
            @Override
            public Hymn save(Hymn hymn) {
                return hymn;
            }

            @Override
            public List<Hymn> findEnabled() {
                return List.of();
            }

            @Override
            public Optional<Hymn> findById(UUID id) {
                return Optional.empty();
            }

            @Override
            public List<Hymn> findAll() {
                return Collections.emptyList();
            }

            @Override
            public List<Hymn> findByIdIn(List<UUID> ids) {
                batchCalls.incrementAndGet();
                return List.of(
                    new Hymn(hymnA, "A", "1", "tag", true, Instant.now()),
                    new Hymn(hymnB, "B", "2", "tag", true, Instant.now())
                );
            }
        };

        GetHistoryUseCase useCase = new GetHistoryUseCase(stateRepository, hymnRepository);
        List<GetHistoryUseCase.HistoryItem> result = useCase.getHistory(userId);

        assertThat(batchCalls.get()).isEqualTo(1);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).hymn().id()).isEqualTo(hymnB);
        assertThat(result.get(1).hymn().id()).isEqualTo(hymnA);
    }
}
