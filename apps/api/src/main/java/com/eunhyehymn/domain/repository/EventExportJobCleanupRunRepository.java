package com.eunhyehymn.domain.repository;

import com.eunhyehymn.domain.model.EventExportJobCleanupRun;
import java.time.Instant;

public interface EventExportJobCleanupRunRepository {
    EventExportJobCleanupRun save(EventExportJobCleanupRun run);

    Summary summarize(Instant fromInclusive, Instant toExclusive);

    record Summary(
        long runCount,
        long deletedJobCount
    ) {
    }
}
