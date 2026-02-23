package com.eunhyehymn.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.eunhyehymn.application.ports.HymnRecommendationClient;
import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendHymnsUseCaseTest {
    @Test
    void usesFallbackWhenAiResponseHasNoUsableRecommendations() {
        UUID hymnId = UUID.randomUUID();
        Hymn hymn = new Hymn(hymnId, "Grace Song", "101", "grace", true, Instant.now());
        HymnRepository hymnRepository = new InMemoryHymnRepository(List.of(hymn));

        RecommendHymnsUseCase useCase = new RecommendHymnsUseCase(
            hymnRepository,
            (situation, candidates, maxResults) -> List.of(),
            25,
            3,
            180
        );

        RecommendHymnsUseCase.Result result = useCase.recommend("morning prayer", 2);

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(hymnId);
        assertThat(result.items().get(0).reason()).isEqualTo("Recommended from available hymns.");
    }

    @Test
    void keepsAiRecommendationsWhenValidIdsAreReturned() {
        UUID hymnId = UUID.randomUUID();
        Hymn hymn = new Hymn(hymnId, "Hope Song", "102", "hope", true, Instant.now());
        HymnRepository hymnRepository = new InMemoryHymnRepository(List.of(hymn));

        HymnRecommendationClient recommendationClient = (situation, candidates, maxResults) -> List.of(
            new HymnRecommendationClient.Recommendation(hymnId, "Fits the requested mood.")
        );

        RecommendHymnsUseCase useCase = new RecommendHymnsUseCase(
            hymnRepository,
            recommendationClient,
            25,
            3,
            180
        );

        RecommendHymnsUseCase.Result result = useCase.recommend("hopeful service", 3);

        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).reason()).isEqualTo("Fits the requested mood.");
    }

    private static final class InMemoryHymnRepository implements HymnRepository {
        private final List<Hymn> hymns;

        private InMemoryHymnRepository(List<Hymn> hymns) {
            this.hymns = hymns;
        }

        @Override
        public Hymn save(Hymn hymn) {
            return hymn;
        }

        @Override
        public List<Hymn> findEnabled() {
            return hymns;
        }

        @Override
        public List<Hymn> findAll() {
            return hymns;
        }

        @Override
        public Optional<Hymn> findById(UUID id) {
            return hymns.stream().filter(hymn -> hymn.id().equals(id)).findFirst();
        }

        @Override
        public List<Hymn> findByIdIn(List<UUID> ids) {
            return hymns.stream().filter(hymn -> ids.contains(hymn.id())).toList();
        }

        @Override
        public void deleteById(UUID id) {
        }
    }
}
