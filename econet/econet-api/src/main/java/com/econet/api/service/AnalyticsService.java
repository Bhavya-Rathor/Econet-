package com.econet.api.service;

import com.econet.api.model.EnvironmentalReading;
import com.econet.api.model.IncidentAlert;
import com.econet.api.model.MonitoringZone;
import com.econet.api.repository.EnvironmentalReadingRepository;
import com.econet.api.repository.IncidentAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private EnvironmentalReadingRepository readingRepository;

    @Autowired
    private IncidentAlertRepository alertRepository;

    @Autowired
    private EnvironmentalTelemetryService telemetryService;

    public Map<String, Object> getLiveDashboardSummary() {
        MonitoringZone zone = telemetryService.getDefaultZone();
        Long zoneId = zone.getId();

        // 1. Get latest readings for the zone
        List<EnvironmentalReading> latestReadings = readingRepository.findLatestReadingsByZone(zoneId);
        Map<String, Object> readingsMap = new HashMap<>();
        double currentAqi = 85.0;
        double currentSoilMoisture = 28.0;
        double currentWaterPh = 7.0;
        double currentNoise = 65.0;

        for (EnvironmentalReading r : latestReadings) {
            Map<String, Object> item = new HashMap<>();
            item.put("value", r.getReadingValue());
            item.put("unit", r.getUnit());
            item.put("status", r.getStatus());
            item.put("domain", r.getDomain());
            item.put("recordedAt", r.getRecordedAt());
            readingsMap.put(r.getParameterName(), item);

            if ("AQI".equals(r.getParameterName())) currentAqi = r.getReadingValue();
            if ("SOIL_MOISTURE".equals(r.getParameterName())) currentSoilMoisture = r.getReadingValue();
            if ("WATER_PH".equals(r.getParameterName())) currentWaterPh = r.getReadingValue();
            if ("NOISE_DB".equals(r.getParameterName())) currentNoise = r.getReadingValue();
        }

        // 2. Active unresolved alerts
        List<IncidentAlert> activeAlerts = alertRepository.findByZoneIdAndResolvedFalseOrderByTriggeredAtDesc(zoneId);

        // 3. Compute Composite Eco-Health Index (Score out of 100)
        double aqiScore = Math.max(0, 100 - (currentAqi / 2.5)); // AQI 50 -> 80, AQI 200 -> 20
        double waterScore = Math.max(0, 100 - (Math.abs(currentWaterPh - 7.0) * 35)); // pH 7 -> 100, pH 5.5 -> ~47
        double soilScore = Math.min(100, (currentSoilMoisture / 35.0) * 100); // 35% -> 100
        double noiseScore = Math.max(0, 100 - ((currentNoise - 40) * 1.5)); // 50dB -> 85

        double compositeScore = (aqiScore * 0.35) + (waterScore * 0.25) + (soilScore * 0.25) + (noiseScore * 0.15);
        int finalScore = (int) Math.round(Math.max(10, Math.min(100, compositeScore)));

        String overallHealth;
        if (finalScore >= 80) overallHealth = "OPTIMAL";
        else if (finalScore >= 60) overallHealth = "MODERATE";
        else overallHealth = "DEGRADED / CRITICAL";

        Map<String, Object> response = new HashMap<>();
        response.put("zone", Map.of(
                "id", zone.getId(),
                "name", zone.getName(),
                "type", zone.getZoneType(),
                "coordinates", Map.of("lat", zone.getLatitude(), "lng", zone.getLongitude()),
                "description", zone.getDescription()
        ));
        response.put("ecoHealthScore", finalScore);
        response.put("overallHealthStatus", overallHealth);
        response.put("activeAlertsCount", activeAlerts.size());
        response.put("activeAlerts", activeAlerts);
        response.put("latestReadings", readingsMap);
        response.put("systemTime", LocalDateTime.now());

        return response;
    }

    public Map<String, Object> generateDailyAuditReport() {
        MonitoringZone zone = telemetryService.getDefaultZone();
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<Object[]> rawStats = readingRepository.get24HourStatistics(zone.getId(), since);
        Map<String, Object> parameterStats = new HashMap<>();

        for (Object[] row : rawStats) {
            String param = (String) row[0];
            Double avgVal = row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0.0;
            Double minVal = row[2] != null ? Math.round(((Number) row[2]).doubleValue() * 10.0) / 10.0 : 0.0;
            Double maxVal = row[3] != null ? Math.round(((Number) row[3]).doubleValue() * 10.0) / 10.0 : 0.0;
            Long sampleCount = row[4] != null ? ((Number) row[4]).longValue() : 0L;

            parameterStats.put(param, Map.of(
                    "avg", avgVal,
                    "min", minVal,
                    "max", maxVal,
                    "samples", sampleCount
            ));
        }

        long totalAlerts = alertRepository.count();
        long activeAlerts = alertRepository.countByZoneIdAndResolvedFalse(zone.getId());

        Map<String, Object> report = new HashMap<>();
        report.put("reportTitle", "EcoNet 24-Hour Environmental Audit Bulletin");
        report.put("zoneName", zone.getName());
        report.put("generatedAt", LocalDateTime.now());
        report.put("windowHours", 24);
        report.put("parameterStatistics", parameterStats);
        report.put("totalAlertsLogged", totalAlerts);
        report.put("activeUnresolvedAlerts", activeAlerts);
        report.put("cloudStorageStatus", "SYNCHRONIZED");

        return report;
    }
}
