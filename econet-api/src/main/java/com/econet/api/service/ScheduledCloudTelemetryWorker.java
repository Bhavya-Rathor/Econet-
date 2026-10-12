package com.econet.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledCloudTelemetryWorker {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledCloudTelemetryWorker.class);

    @Autowired
    private EnvironmentalTelemetryService telemetryService;

    // Runs automatically every 45 seconds (45,000 ms) in the cloud background
    @Scheduled(fixedRate = 45000, initialDelay = 10000)
    public void executeAutomatedTelemetryIngestion() {
        try {
            logger.info("[CLOUD WORKER] Triggering scheduled hyper-local telemetry ingestion cycle...");
            var readings = telemetryService.ingestLiveTelemetryBatch(null);
            logger.info("[CLOUD WORKER] Successfully ingested {} readings across Air, Water, Soil, and Pollution domains.", readings.size());
        } catch (Exception e) {
            logger.error("[CLOUD WORKER] Error in automated telemetry ingestion cycle: {}", e.getMessage());
        }
    }
}
