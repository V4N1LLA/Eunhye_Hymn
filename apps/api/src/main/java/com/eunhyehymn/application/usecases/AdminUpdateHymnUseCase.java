package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminUpdateHymnUseCase {
    private final HymnRepository hymnRepository;

    public AdminUpdateHymnUseCase(HymnRepository hymnRepository) {
        this.hymnRepository = hymnRepository;
    }

    public Hymn update(UUID hymnId, String title, String number, String tags, Boolean enabled) {
        if (title == null && number == null && tags == null && enabled == null) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "empty_update",
                "at least one field is required",
                null
            );
        }

        Hymn existing = hymnRepository.findById(hymnId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "hymn_not_found", "hymn not found", null));

        String effectiveTitle = title != null ? normalizeRequired(title, "title") : existing.title();
        String effectiveNumber = number != null ? normalizeOptional(number) : existing.number();
        String effectiveTags = tags != null ? normalizeOptional(tags) : existing.tags();
        boolean effectiveEnabled = enabled != null ? enabled : existing.enabled();

        if (
            Objects.equals(effectiveTitle, existing.title())
                && Objects.equals(effectiveNumber, existing.number())
                && Objects.equals(effectiveTags, existing.tags())
                && effectiveEnabled == existing.enabled()
        ) {
            return existing;
        }

        Hymn updated = new Hymn(
            existing.id(),
            effectiveTitle,
            effectiveNumber,
            effectiveTags,
            effectiveEnabled,
            existing.createdAt()
        );

        return hymnRepository.save(updated);
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "validation_error",
                fieldName + " is required",
                null
            );
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
