package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.application.usecases.RecordEventsUseCase;
import com.eunhyehymn.application.usecases.RecordEventsUseCase.EventInput;
import com.eunhyehymn.common.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@Validated
public class EventController {
    private final RecordEventsUseCase recordEventsUseCase;
    private final ObjectMapper objectMapper;

    public EventController(RecordEventsUseCase recordEventsUseCase, ObjectMapper objectMapper) {
        this.recordEventsUseCase = recordEventsUseCase;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ApiResponse<Void> record(@RequestBody JsonNode body, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<EventInput> inputs = parseInputs(body);
        recordEventsUseCase.record(userId, inputs);
        return ApiResponse.success(null);
    }

    private List<EventInput> parseInputs(JsonNode body) {
        if (body.isArray()) {
            return objectMapper.convertValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, EventInput.class));
        }
        EventInput single = objectMapper.convertValue(body, EventInput.class);
        return List.of(single);
    }
}
