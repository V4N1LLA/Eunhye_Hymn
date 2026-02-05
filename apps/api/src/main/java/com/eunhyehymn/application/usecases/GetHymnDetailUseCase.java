package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Asset;
import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.model.UserHymnState;
import com.eunhyehymn.domain.repository.AssetRepository;
import com.eunhyehymn.domain.repository.HymnRepository;
import com.eunhyehymn.domain.repository.UserHymnStateRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class GetHymnDetailUseCase {
    private final HymnRepository hymnRepository;
    private final AssetRepository assetRepository;
    private final UserHymnStateRepository userHymnStateRepository;

    public GetHymnDetailUseCase(
        HymnRepository hymnRepository,
        AssetRepository assetRepository,
        UserHymnStateRepository userHymnStateRepository
    ) {
        this.hymnRepository = hymnRepository;
        this.assetRepository = assetRepository;
        this.userHymnStateRepository = userHymnStateRepository;
    }

    public HymnDetail getDetail(UUID hymnId, UUID userId) {
        Hymn hymn = hymnRepository.findById(hymnId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "hymn_not_found", "찬송가를 찾을 수 없습니다", null));

        List<Asset> assets = assetRepository.findByHymnId(hymnId);
        Instant now = Instant.now();

        UserHymnState updated = userHymnStateRepository.findByUserIdAndHymnId(userId, hymnId)
            .map(existing -> new UserHymnState(
                existing.userId(),
                existing.hymnId(),
                existing.favorite(),
                now,
                existing.lastPartPlayed(),
                existing.lastPlayPositionMs()
            ))
            .orElseGet(() -> new UserHymnState(userId, hymnId, false, now, null, null));

        userHymnStateRepository.save(updated);

        return new HymnDetail(hymn, assets, updated.lastOpenedAt());
    }

    public record HymnDetail(Hymn hymn, List<Asset> assets, Instant lastOpenedAt) {
    }
}
