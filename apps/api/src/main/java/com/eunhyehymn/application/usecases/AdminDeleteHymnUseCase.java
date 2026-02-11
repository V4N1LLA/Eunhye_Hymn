package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.repository.AssetRepository;
import com.eunhyehymn.domain.repository.EventRepository;
import com.eunhyehymn.domain.repository.HymnNoteRepository;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminDeleteHymnUseCase {
    private final HymnRepository hymnRepository;
    private final AssetRepository assetRepository;
    private final HymnNoteRepository hymnNoteRepository;
    private final UserHymnStateRepository userHymnStateRepository;
    private final EventRepository eventRepository;

    public AdminDeleteHymnUseCase(
        HymnRepository hymnRepository,
        AssetRepository assetRepository,
        HymnNoteRepository hymnNoteRepository,
        UserHymnStateRepository userHymnStateRepository,
        EventRepository eventRepository
    ) {
        this.hymnRepository = hymnRepository;
        this.assetRepository = assetRepository;
        this.hymnNoteRepository = hymnNoteRepository;
        this.userHymnStateRepository = userHymnStateRepository;
        this.eventRepository = eventRepository;
    }

    public void delete(UUID hymnId) {
        hymnRepository.findById(hymnId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "hymn_not_found", "찬송가를 찾을 수 없습니다", null));

        // FK 제약조건에 ON DELETE CASCADE가 없으므로 관련 데이터를 먼저 삭제
        eventRepository.deleteByHymnId(hymnId);
        userHymnStateRepository.deleteByHymnId(hymnId);
        hymnNoteRepository.deleteByHymnId(hymnId);
        assetRepository.deleteByHymnId(hymnId);
        hymnRepository.deleteById(hymnId);
    }
}
