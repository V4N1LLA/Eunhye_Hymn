package com.eunhyehymn.presentation.controllers;

import com.eunhyehymn.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    @GetMapping("/ping")
    public ApiResponse<Map<String, Boolean>> ping() {
        return ApiResponse.success(Map.of("ok", true));
    }
}
