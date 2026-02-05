package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminUpdateHymnUseCase {
    private final HymnRepository hymnRepository;

    public AdminUpdateHymnUseCase(HymnRepository hymnRepository) {
        this.hymnRepository = hymnRepository;
    }

    public Hymn update(UUID hymnId, String title, String number, String tags, Boolean enabled) {
        Hymn existing = hymnRepository.findById(hymnId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "hymn_not_found", "찬송가를 찾을 수 없습니다", null));

        Hymn updated = new Hymn(
            existing.id(),
            title != null ? title : existing.title(),
            number != null ? number : existing.number(),
            tags != null ? tags : existing.tags(),
            enabled != null ? enabled : existing.enabled(),
            existing.createdAt()
        );

        return hymnRepository.save(updated);
    }
}
