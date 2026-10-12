package com.econet.api.controller;

import com.econet.api.dto.ApiResponse;
import com.econet.api.service.WeatherPipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST endpoints driving the PostgreSQL <-> Hadoop MapReduce weather pipeline.
 * See weather-analytics/run_pipeline.ps1 for the end-to-end sequence.
 */
@RestController
@RequestMapping("/api/weather/pipeline")
@CrossOrigin(origins = "*")
public class WeatherPipelineController {

    private static final String DEFAULT_START = "2026-04-01";
    private static final String DEFAULT_END = "2026-09-30";
    private static final String DEFAULT_MODEL = "SEASONAL_HARMONIC_V1";

    @Autowired
    private WeatherPipelineService pipelineService;

    // 1. Pull real daily observations from Open-Meteo into weather_daily
    @PostMapping("/backfill")
    public ResponseEntity<ApiResponse> backfill(
            @RequestParam(defaultValue = DEFAULT_START) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(defaultValue = DEFAULT_END) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            return ResponseEntity.ok(new ApiResponse(true, "Backfill complete", pipelineService.backfillFromOpenMeteo(start, end)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Backfill failed: " + e.getMessage(), null));
        }
    }

    // 2. Download weather_daily as the .dat input file for HDFS / MapReduce
    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> export(
            @RequestParam(defaultValue = DEFAULT_START) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(defaultValue = DEFAULT_END) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"weather_data.dat\"")
                .body(pipelineService.exportDat(start, end));
    }

    // 3. Upload the MapReduce output (part-r-00000) into weather_monthly_summary
    @PostMapping(value = "/import", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ApiResponse> importOutput(@RequestBody String partFileContent) {
        try {
            var rows = pipelineService.importMapReduceOutput(partFileContent);
            return ResponseEntity.ok(new ApiResponse(true, "Imported " + rows.size() + " summary rows", pipelineService.getHadoopSummaries()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Import failed: " + e.getMessage(), null));
        }
    }

    // 5. Upload a forecast CSV (from train_forecast.py) into weather_forecast
    @PostMapping(value = "/forecast/import", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<ApiResponse> importForecast(
            @RequestParam(defaultValue = DEFAULT_MODEL) String model,
            @RequestBody String csv) {
        try {
            int saved = pipelineService.importForecastCsv(csv, model);
            return ResponseEntity.ok(new ApiResponse(true, "Imported " + saved + " forecast days for " + model, null));
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Forecast import failed: " + e.getMessage(), null));
        }
    }

    // Stored forecast
    @GetMapping("/forecast")
    public ResponseEntity<ApiResponse> forecast(@RequestParam(defaultValue = DEFAULT_MODEL) String model) {
        return ResponseEntity.ok(new ApiResponse(true, "Forecast for " + model, pipelineService.getForecast(model)));
    }

    // Stored Hadoop results
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> summary() {
        return ResponseEntity.ok(new ApiResponse(true, "Hadoop MapReduce summaries", pipelineService.getHadoopSummaries()));
    }

    // 4. Compare Hadoop results with the same aggregation done in SQL (GROUP BY ROLLUP)
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verify(
            @RequestParam(defaultValue = DEFAULT_START) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(defaultValue = DEFAULT_END) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        var result = pipelineService.verifyAgainstSql(start, end);
        boolean ok = Boolean.TRUE.equals(result.get("allMatch"));
        return ResponseEntity.ok(new ApiResponse(ok, ok ? "Hadoop output matches SQL aggregation" : "Hadoop output does NOT match SQL aggregation", result));
    }
}
