package com.eunhyehymn.infrastructure.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eunhyehymn.application.usecases.GetEventExportOpsMetricsUseCase;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventExportJobOpsAlertSchedulerTest {
    @Test
    void evaluateOpsAlertsUsesConfiguredWindowDays() {
        GetEventExportOpsMetricsUseCase metricsUseCase = mock(GetEventExportOpsMetricsUseCase.class);
        when(metricsUseCase.execute(7)).thenReturn(new GetEventExportOpsMetricsUseCase.Result(
            7,
            Instant.parse("2026-02-13T00:00:00Z"),
            Instant.parse("2026-02-14T00:00:00Z"),
            new GetEventExportOpsMetricsUseCase.JobMetrics(3, 0, 0, 3, 0, 0.0),
            new GetEventExportOpsMetricsUseCase.ProcessingMetrics(3, 12.0, 20.0),
            new GetEventExportOpsMetricsUseCase.CleanupMetrics(1, 5)
        ));

        EventExportJobOpsAlertScheduler scheduler = new EventExportJobOpsAlertScheduler(
            metricsUseCase,
            7,
            5,
            20,
            20.0,
            120.0
        );
        scheduler.evaluateOpsAlerts();

        verify(metricsUseCase).execute(7);
    }
}
