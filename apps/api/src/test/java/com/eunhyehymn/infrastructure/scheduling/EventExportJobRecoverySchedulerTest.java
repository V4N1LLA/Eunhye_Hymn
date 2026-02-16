package com.eunhyehymn.infrastructure.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eunhyehymn.application.usecases.AdminEventExportJobUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

class EventExportJobRecoverySchedulerTest {
    @Test
    void recoverAndDispatchRequeuesStaleJobsAndDispatchesQueuedJobs() {
        AdminEventExportJobUseCase useCase = mock(AdminEventExportJobUseCase.class);
        TaskExecutor executor = mock(TaskExecutor.class);
        UUID jobA = UUID.randomUUID();
        UUID jobB = UUID.randomUUID();
        when(useCase.requeueStaleRunningJobs(any())).thenReturn(2L);
        when(useCase.findQueuedJobIds(20)).thenReturn(List.of(jobA, jobB));

        EventExportJobRecoveryScheduler scheduler = new EventExportJobRecoveryScheduler(useCase, executor, 15, 20);
        scheduler.recoverAndDispatch();

        verify(useCase, times(1)).requeueStaleRunningJobs(any());
        verify(useCase, times(1)).findQueuedJobIds(20);
        verify(executor, times(2)).execute(any(Runnable.class));
    }

    @Test
    void recoverAndDispatchStopsDispatchingWhenQueueRejectsTask() {
        AdminEventExportJobUseCase useCase = mock(AdminEventExportJobUseCase.class);
        TaskExecutor executor = mock(TaskExecutor.class);
        when(useCase.requeueStaleRunningJobs(any())).thenReturn(0L);
        when(useCase.findQueuedJobIds(20)).thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));
        doThrow(new TaskRejectedException("queue saturated")).when(executor).execute(any(Runnable.class));

        EventExportJobRecoveryScheduler scheduler = new EventExportJobRecoveryScheduler(useCase, executor, 15, 20);
        scheduler.recoverAndDispatch();

        verify(executor, times(1)).execute(any(Runnable.class));
    }
}
