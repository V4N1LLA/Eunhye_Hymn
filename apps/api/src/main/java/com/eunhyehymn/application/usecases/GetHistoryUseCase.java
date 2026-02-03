package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.model.UserHymnState;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GetHistoryUseCase {
    private final UserHymnStateRepository userHymnStateRepository;
    private final HymnRepository hymnRepository;

    public GetHistoryUseCase(UserHymnStateRepository userHymnStateRepository, HymnRepository hymnRepository) {
        this.userHymnStateRepository = userHymnStateRepository;
        this.hymnRepository = hymnRepository;
    }

    public List<HistoryItem> getHistory(UUID userId) {
        List<UserHymnState> states = userHymnStateRepository.findByUserIdOrderByLastOpenedAtDesc(userId);
        List<HistoryItem> result = new ArrayList<>();
        for (UserHymnState state : states) {
            hymnRepository.findById(state.hymnId()).ifPresent(hymn ->
                result.add(new HistoryItem(hymn, state.lastOpenedAt()))
            );
        }
        return result;
    }

    public record HistoryItem(Hymn hymn, java.time.Instant lastOpenedAt) {
    }
}
