package com.eunhyehymn.presentation.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eunhyehymn.application.usecases.AdminEventExportJobUseCase;
import com.eunhyehymn.application.usecases.AdminListEventsUseCase;
import com.eunhyehymn.application.usecases.GetEventExportOpsMetricsUseCase;
import com.eunhyehymn.common.response.ApiResponse;
import com.eunhyehymn.domain.model.EventExportJob;
import com.eunhyehymn.domain.model.EventExportJobStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AdminEventControllerTest {
    @Test
    void createExportJobReturnsAcceptedWhenTaskDispatchIsRejected() {
        AdminListEventsUseCase listEventsUseCase = mock(AdminListEventsUseCase.class);
        AdminEventExportJobUseCase exportJobUseCase = mock(AdminEventExportJobUseCase.class);
        GetEventExportOpsMetricsUseCase metricsUseCase = mock(GetEventExportOpsMetricsUseCase.class);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);

        UUID requestedBy = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        EventExportJob queuedJob = new EventExportJob(
            jobId,
            requestedBy,
            null,
            null,
            null,
            null,
            Instant.now(),
            10_000,
            EventExportJobStatus.QUEUED,
            null,
            null,
            null,
            null,
            Instant.now(),
            null,
            null
        );

        when(exportJobUseCase.create(any())).thenReturn(queuedJob);
        doThrow(new TaskRejectedException("queue saturated")).when(taskExecutor).execute(any(Runnable.class));

        AdminEventController controller = new AdminEventController(
            listEventsUseCase,
            exportJobUseCase,
            metricsUseCase,
            taskExecutor
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(requestedBy.toString(), "N/A");

        ResponseEntity<ApiResponse<AdminEventController.EventExportJobResponse>> response = controller.createExportJob(
            null,
            null,
            null,
            null,
            null,
            10_000,
            authentication
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().id()).isEqualTo(jobId);
        verify(taskExecutor).execute(any(Runnable.class));
    }
}
