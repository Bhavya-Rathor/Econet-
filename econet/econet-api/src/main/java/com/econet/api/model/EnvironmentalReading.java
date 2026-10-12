package com.econet.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "environmental_readings", indexes = {
    @Index(name = "idx_readings_zone_time", columnList = "zone_id, recordedAt"),
    @Index(name = "idx_readings_param", columnList = "parameterName, recordedAt")
})
public class EnvironmentalReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id", nullable = false)
    private MonitoringZone zone;

    @Column(nullable = false, length = 30)
    private String domain; // "AIR", "WATER", "SOIL", "POLLUTION"

    @Column(nullable = false, length = 50)
    private String parameterName; // e.g. "AQI", "PM2_5", "WATER_PH", "SOIL_MOISTURE", "NOISE_DB"

    @Column(nullable = false)
    private Double readingValue;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, length = 20)
    private String status = "NORMAL"; // "NORMAL", "WARNING", "CRITICAL"

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public EnvironmentalReading() {}

    public EnvironmentalReading(MonitoringZone zone, String domain, String parameterName, Double readingValue, String unit, String status, LocalDateTime recordedAt) {
        this.zone = zone;
        this.domain = domain;
        this.parameterName = parameterName;
        this.readingValue = readingValue;
        this.unit = unit;
        this.status = status;
        this.recordedAt = recordedAt != null ? recordedAt : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MonitoringZone getZone() { return zone; }
    public void setZone(MonitoringZone zone) { this.zone = zone; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public Double getReadingValue() { return readingValue; }
    public void setReadingValue(Double readingValue) { this.readingValue = readingValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
