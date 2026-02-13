package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.AdminListEventsUseCase;
import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/events")
@Validated
public class AdminEventController {
    private final AdminListEventsUseCase adminListEventsUseCase;

    public AdminEventController(AdminListEventsUseCase adminListEventsUseCase) {
        this.adminListEventsUseCase = adminListEventsUseCase;
    }

    @GetMapping
    public ApiResponse<EventListResponse> list(
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String hymnId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) Integer limit,
        @RequestParam(required = false) Integer summaryDays
    ) {
        QueryContext context = resolveQueryContext(eventType, userId, hymnId, from, to, page, size, limit, summaryDays);

        AdminListEventsUseCase.Result result = adminListEventsUseCase.execute(new AdminListEventsUseCase.Query(
            context.fromInclusive(),
            context.toExclusive(),
            context.eventType(),
            context.userId(),
            context.hymnId(),
            context.page(),
            context.size(),
            context.summaryDays()
        ));

        List<EventItemResponse> items = result.items().stream().map(this::toItem).toList();
        List<EventTypeCountResponse> byType = result.summary().byType().stream()
            .map(row -> new EventTypeCountResponse(row.eventType().name(), row.count()))
            .toList();

        SummaryResponse summary = new SummaryResponse(
            result.summary().fromInclusive(),
            result.summary().toExclusive(),
            result.summary().total(),
            byType
        );

        PaginationResponse pagination = new PaginationResponse(
            result.pagination().page(),
            result.pagination().size(),
            result.pagination().total(),
            result.pagination().totalPages(),
            result.pagination().hasPrevious(),
            result.pagination().hasNext()
        );

        return ApiResponse.success(new EventListResponse(items, summary, pagination));
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String hymnId,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) Integer limit
    ) {
        QueryContext context = resolveQueryContext(eventType, userId, hymnId, from, to, null, null, null, null);
        List<Event> items = adminListEventsUseCase.listForExport(new AdminListEventsUseCase.Query(
            context.fromInclusive(),
            context.toExclusive(),
            context.eventType(),
            context.userId(),
            context.hymnId(),
            1,
            context.size(),
            context.summaryDays()
        ), limit);

        String csv = toCsv(items);
        String filename = "admin-events-" + Instant.now().toString().replace(":", "-") + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    private QueryContext resolveQueryContext(
        String eventType,
        String userId,
        String hymnId,
        String from,
        String to,
        Integer page,
        Integer size,
        Integer limit,
        Integer summaryDays
    ) {
        EventType parsedEventType = parseEventType(eventType);
        UUID parsedUserId = parseUuid(userId, "userId");
        UUID parsedHymnId = parseUuid(hymnId, "hymnId");
        Instant fromInclusive = parseInstant(from, "from");
        Instant toExclusive = parseInstant(to, "to");

        if (fromInclusive != null && toExclusive != null && !fromInclusive.isBefore(toExclusive)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_range", "'from' must be earlier than 'to'", null);
        }

        Integer resolvedPage = page;
        Integer resolvedSize = size;
        if (resolvedSize == null && limit != null) {
            resolvedSize = limit;
        }
        if (resolvedPage == null && limit != null) {
            resolvedPage = 1;
        }

        return new QueryContext(
            parsedEventType,
            parsedUserId,
            parsedHymnId,
            fromInclusive,
            toExclusive,
            resolvedPage,
            resolvedSize,
            summaryDays
        );
    }

    private String toCsv(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,userId,eventType,hymnId,part,metadataJson,createdAt\n");

        for (Event event : events) {
            sb.append(csv(event.id()));
            sb.append(',').append(csv(event.userId()));
            sb.append(',').append(csv(event.eventType().name()));
            sb.append(',').append(csv(event.hymnId()));
            sb.append(',').append(csv(event.part() == null ? null : event.part().name()));
            sb.append(',').append(csv(event.metadataJson()));
            sb.append(',').append(csv(event.createdAt()));
            sb.append('\n');
        }
        return sb.toString();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String raw = value.toString().replace("\"", "\"\"");
        return "\"" + raw + "\"";
    }

    private EventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return EventType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_event_type", "Invalid eventType value", null);
        }
    }

    private UUID parseUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_uuid", "Invalid UUID for " + fieldName, null);
        }
    }

    private Instant parseInstant(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_datetime",
                fieldName + " must be ISO-8601 UTC format",
                null
            );
        }
    }

    private EventItemResponse toItem(Event event) {
        return new EventItemResponse(
            event.id(),
            event.userId(),
            event.eventType().name(),
            event.hymnId(),
            event.part() == null ? null : event.part().name(),
            event.metadataJson(),
            event.createdAt()
        );
    }

    public record EventListResponse(List<EventItemResponse> items, SummaryResponse summary, PaginationResponse pagination) {
    }

    public record EventItemResponse(
        UUID id,
        UUID userId,
        String eventType,
        UUID hymnId,
        String part,
        String metadataJson,
        Instant createdAt
    ) {
    }

    public record SummaryResponse(
        Instant fromInclusive,
        Instant toExclusive,
        long total,
        List<EventTypeCountResponse> byType
    ) {
    }

    public record EventTypeCountResponse(String eventType, long count) {
    }

    public record PaginationResponse(
        int page,
        int size,
        long total,
        long totalPages,
        boolean hasPrevious,
        boolean hasNext
    ) {
    }

    private record QueryContext(
        EventType eventType,
        UUID userId,
        UUID hymnId,
        Instant fromInclusive,
        Instant toExclusive,
        Integer page,
        Integer size,
        Integer summaryDays
    ) {
    }
}
