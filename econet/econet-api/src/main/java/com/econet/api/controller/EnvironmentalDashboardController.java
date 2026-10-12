package com.econet.api.controller;

import com.econet.api.model.EnvironmentalReading;
import com.econet.api.model.IncidentAlert;
import com.econet.api.model.MonitoringZone;
import com.econet.api.repository.EnvironmentalReadingRepository;
import com.econet.api.repository.IncidentAlertRepository;
import com.econet.api.service.AnalyticsService;
import com.econet.api.service.EnvironmentalTelemetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/environment")
@CrossOrigin(origins = "*")
public class EnvironmentalDashboardController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private EnvironmentalTelemetryService telemetryService;

    @Autowired
    private EnvironmentalReadingRepository readingRepository;

    @Autowired
    private IncidentAlertRepository alertRepository;

    // 1. Full Live Dashboard (Composite Score + 4 Domains + Active Alerts)
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getLiveDashboard() {
        return ResponseEntity.ok(analyticsService.getLiveDashboardSummary());
    }

    // 2. Latest Sensor Readings per Parameter
    @GetMapping("/readings/live")
    public ResponseEntity<List<EnvironmentalReading>> getLatestReadings() {
        MonitoringZone zone = telemetryService.getDefaultZone();
        return ResponseEntity.ok(readingRepository.findLatestReadingsByZone(zone.getId()));
    }

    // 3. Historical Telemetry for Frontend Charts / Trends
    @GetMapping("/readings/history")
    public ResponseEntity<List<EnvironmentalReading>> getReadingHistory(
            @RequestParam(required = false) String domain,
            @RequestParam(defaultValue = "30") int limit) {
        MonitoringZone zone = telemetryService.getDefaultZone();
        int safeLimit = Math.min(limit, 200);
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        if (domain != null && !domain.trim().isEmpty()) {
            return ResponseEntity.ok(readingRepository.findByZoneIdAndDomainOrderByRecordedAtDesc(
                    zone.getId(), domain.toUpperCase().trim(), pageRequest));
        }
        return ResponseEntity.ok(readingRepository.findByZoneIdOrderByRecordedAtDesc(zone.getId(), pageRequest));
    }

    // 4. Incident Alerts (All)
    @GetMapping("/alerts")
    public ResponseEntity<List<IncidentAlert>> getAllAlerts(
            @RequestParam(defaultValue = "20") int limit) {
        MonitoringZone zone = telemetryService.getDefaultZone();
        return ResponseEntity.ok(alertRepository.findByZoneIdOrderByTriggeredAtDesc(zone.getId(), PageRequest.of(0, limit)));
    }

    // 5. Active Unresolved Alerts
    @GetMapping("/alerts/active")
    public ResponseEntity<List<IncidentAlert>> getActiveAlerts() {
        MonitoringZone zone = telemetryService.getDefaultZone();
        return ResponseEntity.ok(alertRepository.findByZoneIdAndResolvedFalseOrderByTriggeredAtDesc(zone.getId()));
    }

    // 6. Action Protocol Resolution (Productive workflow)
    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<IncidentAlert> resolveAlert(@PathVariable Long id) {
        return alertRepository.findById(id).map(alert -> {
            alert.setResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            return ResponseEntity.ok(alertRepository.save(alert));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 7. Manual Telemetry Simulation Trigger (Ideal for Live Teacher Demos)
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> triggerManualTelemetryIngestion() {
        var readings = telemetryService.ingestLiveTelemetryBatch(null);
        return ResponseEntity.ok(Map.of(
                "message", "Live hyper-local telemetry batch ingested successfully.",
                "readingsCount", readings.size(),
                "timestamp", LocalDateTime.now()
        ));
    }

    // 8. 24-Hour Statistical Analytical Report (DBMS Aggregations)
    @GetMapping("/report/daily")
    public ResponseEntity<Map<String, Object>> getDailyReport() {
        return ResponseEntity.ok(analyticsService.generateDailyAuditReport());
    }

    // 9. Cloud Health Check Probe
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> cloudHealthProbe() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "EcoNet Cloud Telemetry API",
                "environment", "Cloud PaaS Ready",
                "timestamp", LocalDateTime.now()
        ));
    }
}
