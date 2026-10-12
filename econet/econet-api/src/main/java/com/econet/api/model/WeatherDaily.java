package com.econet.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per zone per day of observed weather (source: Open-Meteo historical archive).
 * This is the input dataset exported to Hadoop MapReduce.
 */
@Entity
@Table(name = "weather_daily",
       uniqueConstraints = @UniqueConstraint(name = "uq_weather_daily_zone_date", columnNames = {"zone_id", "obs_date"}),
       indexes = @Index(name = "idx_weather_daily_date", columnList = "obs_date"))
public class WeatherDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private MonitoringZone zone;

    @Column(name = "obs_date", nullable = false)
    private LocalDate obsDate;

    @Column(nullable = false)
    private Double tempMean; // deg C

    @Column(nullable = false)
    private Double rainfallMm;

    @Column(nullable = false)
    private Double humidityPct;

    @Column(nullable = false)
    private Double pressureHpa; // mean sea level

    @Column(nullable = false)
    private Double windSpeedKmh;

    @Column(nullable = false)
    private Double windDirDeg; // dominant direction, 0-360

    @Column(nullable = false, length = 30)
    private String source = "OPEN_METEO_ARCHIVE";

    @Column(nullable = false)
    private LocalDateTime ingestedAt = LocalDateTime.now();

    public WeatherDaily() {}

    public WeatherDaily(MonitoringZone zone, LocalDate obsDate) {
        this.zone = zone;
        this.obsDate = obsDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MonitoringZone getZone() { return zone; }
    public void setZone(MonitoringZone zone) { this.zone = zone; }

    public LocalDate getObsDate() { return obsDate; }
    public void setObsDate(LocalDate obsDate) { this.obsDate = obsDate; }

    public Double getTempMean() { return tempMean; }
    public void setTempMean(Double tempMean) { this.tempMean = tempMean; }

    public Double getRainfallMm() { return rainfallMm; }
    public void setRainfallMm(Double rainfallMm) { this.rainfallMm = rainfallMm; }

    public Double getHumidityPct() { return humidityPct; }
    public void setHumidityPct(Double humidityPct) { this.humidityPct = humidityPct; }

    public Double getPressureHpa() { return pressureHpa; }
    public void setPressureHpa(Double pressureHpa) { this.pressureHpa = pressureHpa; }

    public Double getWindSpeedKmh() { return windSpeedKmh; }
    public void setWindSpeedKmh(Double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }

    public Double getWindDirDeg() { return windDirDeg; }
    public void setWindDirDeg(Double windDirDeg) { this.windDirDeg = windDirDeg; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
}
