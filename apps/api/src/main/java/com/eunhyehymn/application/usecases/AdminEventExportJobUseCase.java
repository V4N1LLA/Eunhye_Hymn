package com.eunhyehymn.application.usecases;

import com.eunhyehymn.common.error.ApiException;
import com.eunhyehymn.domain.model.Event;
import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.domain.model.EventExportJobStatus;
import com.eunhyehymn.domain.model.EventType;
import com.eunhyehymn.domain.repository.EventExportJobRepository;
import com.eunhyehymn.domain.repository.EventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class AdminEventExportJobUseCase {
    private static final int DEFAULT_EXPORT_LIMIT = 20_000;
    private static final int MAX_EXPORT_LIMIT = 100_000;
    private static final int CSV_FETCH_BATCH_SIZE = 1_000;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final String CSV_HEADER = "id,userId,eventType,hymnId,part,metadataJson,createdAt\n";

    private final EventRepository eventRepository;
    private final EventExportJobRepository eventExportJobRepository;

    public AdminEventExportJobUseCase(
        EventRepository eventRepository,
        EventExportJobRepository eventExportJobRepository
    ) {
        this.eventRepository = eventRepository;
        this.eventExportJobRepository = eventExportJobRepository;
    }

    public EventExportJob create(CreateCommand command) {
        int exportLimit = normalizeExportLimit(command.limit());
        EventExportJob job = new EventExportJob(
            UUID.randomUUID(),
            command.requestedBy(),
            command.eventType(),
            command.userId(),
            command.hymnId(),
            command.fromInclusive(),
            command.toExclusive(),
            exportLimit,
            EventExportJobStatus.QUEUED,
            null,
            null,
            null,
            null,
            Instant.now(),
            null,
            null
        );
        return eventExportJobRepository.save(job);
    }

    public EventExportJob get(UUID jobId, UUID requestedBy) {
        EventExportJob job = eventExportJobRepository.findById(jobId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "export_job_not_found",
                "Export job not found",
                null
            ));

        if (!job.requestedBy().equals(requestedBy)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "export_job_not_found", "Export job not found", null);
        }
        return job;
    }

    public EventExportJob process(UUID jobId) {
        EventExportJob queuedJob = eventExportJobRepository.findById(jobId)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "export_job_not_found",
                "Export job not found",
                null
            ));

        if (queuedJob.status() != EventExportJobStatus.QUEUED) {
            return queuedJob;
        }

        EventExportJob runningJob = eventExportJobRepository.save(queuedJob.markRunning(Instant.now()));

        try {
            CsvBuildResult csvBuildResult = buildCsv(runningJob);
            String filename = buildFileName(runningJob.id());
            return eventExportJobRepository.save(runningJob.markCompleted(
                csvBuildResult.rowCount(),
                filename,
                csvBuildResult.csvContent(),
                Instant.now()
            ));
        } catch (Exception ex) {
            String errorMessage = toErrorMessage(ex);
            return eventExportJobRepository.save(runningJob.markFailed(errorMessage, Instant.now()));
        }
    }

    public DownloadResult getDownload(UUID jobId, UUID requestedBy) {
        EventExportJob job = get(jobId, requestedBy);
        if (job.status() == EventExportJobStatus.FAILED) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "export_job_failed",
                job.errorMessage() == null ? "Export job failed" : job.errorMessage(),
                null
            );
        }
        if (job.status() != EventExportJobStatus.COMPLETED) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "export_job_not_ready",
                "Export job is not completed yet",
                null
            );
        }
        if (job.fileName() == null || job.csvContent() == null) {
            throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "export_job_corrupted",
                "Export job result is missing",
                null
            );
        }

        return new DownloadResult(
            job.fileName(),
            job.csvContent()
        );
    }

    private CsvBuildResult buildCsv(EventExportJob job) {
        StringBuilder sb = new StringBuilder(CSV_HEADER);
        int currentPage = 1;
        long rowCount = 0;

        while (rowCount < job.exportLimit()) {
            List<Event> batch = eventRepository.findRecent(
                job.fromInclusive(),
                job.toExclusive(),
                job.eventType(),
                job.userId(),
                job.hymnId(),
                currentPage,
                CSV_FETCH_BATCH_SIZE
            );

            if (batch.isEmpty()) {
                break;
            }

            for (Event event : batch) {
                if (rowCount >= job.exportLimit()) {
                    break;
                }
                appendCsvRow(sb, event);
                rowCount += 1;
            }

            if (batch.size() < CSV_FETCH_BATCH_SIZE) {
                break;
            }

            currentPage += 1;
        }

        return new CsvBuildResult(sb.toString(), rowCount);
    }

    private void appendCsvRow(StringBuilder sb, Event event) {
        sb.append(csv(event.id()));
        sb.append(',').append(csv(event.userId()));
        sb.append(',').append(csv(event.eventType().name()));
        sb.append(',').append(csv(event.hymnId()));
        sb.append(',').append(csv(event.part() == null ? null : event.part().name()));
        sb.append(',').append(csv(event.metadataJson()));
        sb.append(',').append(csv(event.createdAt()));
        sb.append('\n');
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String raw = value.toString().replace("\"", "\"\"");
        return "\"" + raw + "\"";
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

    private String buildFileName(UUID jobId) {
        return "admin-events-async-" + jobId + "-" + Instant.now().toString().replace(":", "-") + ".csv";
    }

    private String toErrorMessage(Exception ex) {
        String raw = ex.getMessage();
        if (raw == null || raw.isBlank()) {
            return "Export job failed unexpectedly";
        }
        if (raw.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return raw;
        }
        return raw.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    public record CreateCommand(
        UUID requestedBy,
        EventType eventType,
        UUID userId,
        UUID hymnId,
        Instant fromInclusive,
        Instant toExclusive,
        Integer limit
    ) {
    }

    public record DownloadResult(
        String fileName,
        String csvContent
    ) {
    }

    private record CsvBuildResult(String csvContent, long rowCount) {
    }
}
