package com.eunhyehymn.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eunhyehymn.application.ports.ExternalAiException;
import com.eunhyehymn.application.ports.HymnRecommendationClient;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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

    @Test
    void throwsBadRequestWhenSituationIsBlank() {
        HymnRepository hymnRepository = new InMemoryHymnRepository(List.of());
        RecommendHymnsUseCase useCase = new RecommendHymnsUseCase(
            hymnRepository,
            (situation, candidates, maxResults) -> List.of(),
            25,
            3,
            180
        );

        assertThatThrownBy(() -> useCase.recommend("   ", 3))
            .isInstanceOf(ApiException.class)
            .satisfies(throwable -> {
                ApiException ex = (ApiException) throwable;
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(ex.getCode()).isEqualTo("validation_error");
            });
    }

    @Test
    void mapsExternalAiExceptionToServiceUnavailable() {
        UUID hymnId = UUID.randomUUID();
        HymnRepository hymnRepository = new InMemoryHymnRepository(List.of(
            new Hymn(hymnId, "Mercy Song", "103", "mercy", true, Instant.now())
        ));
        RecommendHymnsUseCase useCase = new RecommendHymnsUseCase(
            hymnRepository,
            (situation, candidates, maxResults) -> {
                throw new ExternalAiException("gateway timeout");
            },
            25,
            3,
            180
        );

        assertThatThrownBy(() -> useCase.recommend("mercy prayer", 3))
            .isInstanceOf(ApiException.class)
            .satisfies(throwable -> {
                ApiException ex = (ApiException) throwable;
                assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(ex.getCode()).isEqualTo("ai_unavailable");
            });
    }

    @Test
    void clampsRequestedResultsAndSanitizesAiReason() {
        UUID firstHymnId = UUID.randomUUID();
        UUID secondHymnId = UUID.randomUUID();
        HymnRepository hymnRepository = new InMemoryHymnRepository(List.of(
            new Hymn(firstHymnId, "Hope Song", "102", "hope", true, Instant.now()),
            new Hymn(secondHymnId, "Grace Song", "103", "grace", true, Instant.now())
        ));
        String longReason = "r".repeat(200);
        RecommendHymnsUseCase useCase = new RecommendHymnsUseCase(
            hymnRepository,
            (situation, candidates, maxResults) -> List.of(
                new HymnRecommendationClient.Recommendation(firstHymnId, longReason),
                new HymnRecommendationClient.Recommendation(firstHymnId, "duplicate should be ignored"),
                new HymnRecommendationClient.Recommendation(UUID.randomUUID(), "unknown id should be ignored")
            ),
            25,
            3,
            180
        );

        RecommendHymnsUseCase.Result result = useCase.recommend("hopeful service", 10);

        assertThat(result.requestedMaxResults()).isEqualTo(5);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo(firstHymnId);
        assertThat(result.items().get(0).reason()).hasSize(140);
        assertThat(result.fallbackUsed()).isFalse();
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
