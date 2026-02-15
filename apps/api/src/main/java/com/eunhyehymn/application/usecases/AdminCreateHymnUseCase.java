package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Hymn;
import com.eunhyehymn.domain.repository.HymnRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminCreateHymnUseCase {
    private final HymnRepository hymnRepository;

    public AdminCreateHymnUseCase(HymnRepository hymnRepository) {
        this.hymnRepository = hymnRepository;
    }

    public Hymn create(String title, String number, String tags, Boolean enabled) {
        String resolvedTitle = normalizeRequired(title, "title");
        String resolvedNumber = normalizeOptional(number);
        String resolvedTags = normalizeOptional(tags);
        boolean resolvedEnabled = enabled == null || enabled;

        Hymn hymn = new Hymn(
            UUID.randomUUID(),
            resolvedTitle,
            resolvedNumber,
            resolvedTags,
            resolvedEnabled,
            Instant.now()
        );
        return hymnRepository.save(hymn);
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
