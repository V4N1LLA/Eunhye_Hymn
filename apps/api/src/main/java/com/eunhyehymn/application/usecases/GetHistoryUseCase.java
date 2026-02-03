package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.model.UserHymnState;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetHistoryUseCase {
    private final UserHymnStateRepository userHymnStateRepository;
    private final HymnRepository hymnRepository;

    public GetHistoryUseCase(UserHymnStateRepository userHymnStateRepository, HymnRepository hymnRepository) {
        this.userHymnStateRepository = userHymnStateRepository;
        this.hymnRepository = hymnRepository;
    }

    public List<HistoryItem> getHistory(UUID userId) {
        List<UserHymnState> states = userHymnStateRepository.findByUserIdOrderByLastOpenedAtDesc(userId);
        List<UUID> hymnIds = states.stream().map(UserHymnState::hymnId).toList();
        var hymnsById = hymnRepository.findByIdIn(hymnIds).stream()
            .collect(Collectors.toMap(Hymn::id, hymn -> hymn));

        List<HistoryItem> result = new ArrayList<>();
        for (UserHymnState state : states) {
            Hymn hymn = hymnsById.get(state.hymnId());
            if (hymn != null) {
                result.add(new HistoryItem(hymn, state.lastOpenedAt()));
            }
        }
        return result;
    }

    public record HistoryItem(Hymn hymn, java.time.Instant lastOpenedAt) {
    }
}
