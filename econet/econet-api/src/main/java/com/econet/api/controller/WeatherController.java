package com.econet.api.controller;

import com.econet.api.dto.ApiResponse;
import com.econet.api.service.WeatherPipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Read-only weather endpoints used by the dashboard's Weather Analytics tab.
 */
@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "*")
public class WeatherController {

    @Autowired
    private WeatherPipelineService pipelineService;

    // Daily observed weather for charts
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse> daily(
            @RequestParam(defaultValue = "2026-04-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(defaultValue = "2026-09-30") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(new ApiResponse(true, "Daily observed weather", pipelineService.getDaily(start, end)));
    }
}