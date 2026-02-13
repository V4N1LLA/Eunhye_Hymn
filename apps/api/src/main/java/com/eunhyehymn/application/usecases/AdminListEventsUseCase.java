package com.eunhyehymn.application.usecases;

import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.repository.EventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminListEventsUseCase {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;
    private static final int DEFAULT_EXPORT_LIMIT = 1000;
    private static final int MAX_EXPORT_LIMIT = 5000;
    private static final int DEFAULT_SUMMARY_DAYS = 7;
    private static final int MAX_SUMMARY_DAYS = 90;

    private final EventRepository eventRepository;

    public AdminListEventsUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Result execute(Query query) {
        int page = normalizePage(query.page());
        int size = normalizeSize(query.size());
        int summaryDays = normalizeSummaryDays(query.summaryDays());

        List<Event> items = eventRepository.findRecent(
            query.fromInclusive(),
            query.toExclusive(),
            query.eventType(),
            query.userId(),
            query.hymnId(),
            page,
            size
        );
        long filteredTotal = eventRepository.countRecent(
            query.fromInclusive(),
            query.toExclusive(),
            query.eventType(),
            query.userId(),
            query.hymnId()
        );

        Instant summaryTo = Instant.now();
        Instant summaryFrom = summaryTo.minus(summaryDays, ChronoUnit.DAYS);

        Map<EventType, Long> byType = eventRepository.countByEventType(summaryFrom, summaryTo).stream()
            .collect(Collectors.toMap(EventRepository.EventTypeCount::eventType, EventRepository.EventTypeCount::count));

        List<EventTypeCount> counts = Arrays.stream(EventType.values())
            .map(type -> new EventTypeCount(type, byType.getOrDefault(type, 0L)))
            .toList();

        long total = counts.stream().mapToLong(EventTypeCount::count).sum();
        long totalPages = filteredTotal == 0 ? 0 : (filteredTotal + size - 1) / size;
        Pagination pagination = new Pagination(
            page,
            size,
            filteredTotal,
            totalPages,
            page > 1,
            page < totalPages
        );

        return new Result(items, new Summary(summaryFrom, summaryTo, total, counts), pagination);
    }

    public List<Event> listForExport(Query query, Integer limit) {
        int exportLimit = normalizeExportLimit(limit);
        return eventRepository.findRecent(
            query.fromInclusive(),
            query.toExclusive(),
            query.eventType(),
            query.userId(),
            query.hymnId(),
            DEFAULT_PAGE,
            exportLimit
        );
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            return 1;
        }
        return Math.min(size, MAX_SIZE);
    }

    private int normalizeSummaryDays(Integer summaryDays) {
        if (summaryDays == null) {
            return DEFAULT_SUMMARY_DAYS;
        }
        if (summaryDays < 1) {
            return 1;
        }
        return Math.min(summaryDays, MAX_SUMMARY_DAYS);
    }

    private int normalizeExportLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_EXPORT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_EXPORT_LIMIT);
    }

    public record Query(
        Instant fromInclusive,
        Instant toExclusive,
        EventType eventType,
        UUID userId,
        UUID hymnId,
        Integer page,
        Integer size,
        Integer summaryDays
    ) {
    }

    public record EventTypeCount(EventType eventType, long count) {
    }

    public record Summary(Instant fromInclusive, Instant toExclusive, long total, List<EventTypeCount> byType) {
    }

    public record Pagination(
        int page,
        int size,
        long total,
        long totalPages,
        boolean hasPrevious,
        boolean hasNext
    ) {
    }

    public record Result(List<Event> items, Summary summary, Pagination pagination) {
    }
}
