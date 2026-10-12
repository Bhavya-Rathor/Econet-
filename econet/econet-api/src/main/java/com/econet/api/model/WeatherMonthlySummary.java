package com.econet.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Aggregated weather per period, written back from the Hadoop MapReduce job output.
 * periodKey is a month ("2026-04") or "OVERALL" for the whole exported range.
 */
@Entity
@Table(name = "weather_monthly_summary",
       uniqueConstraints = @UniqueConstraint(name = "uq_weather_summary_zone_period_src",
                                             columnNames = {"zone_id", "period_key", "computed_by"}))
public class WeatherMonthlySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private MonitoringZone zone;

    @Column(name = "period_key", nullable = false, length = 20)
    private String periodKey;

    @Column(nullable = false)
    private Integer days;

    @Column(nullable = false)
    private Double avgTemp;

    @Column(nullable = false)
    private Double avgRainfall;

    @Column(nullable = false)
    private Double avgHumidity;

    @Column(nullable = false)
    private Double avgPressure;

    @Column(nullable = false)
    private Double avgWindSpeed;

    @Column(nullable = false)
    private Double meanWindDir; // circular mean

    @Column(name = "computed_by", nullable = false, length = 30)
    private String computedBy = "HADOOP_MAPREDUCE";

    @Column(nullable = false)
    private LocalDateTime computedAt = LocalDateTime.now();

    public WeatherMonthlySummary() {}

    public WeatherMonthlySummary(MonitoringZone zone, String periodKey, String computedBy) {
        this.zone = zone;
        this.periodKey = periodKey;
        this.computedBy = computedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MonitoringZone getZone() { return zone; }
    public void setZone(MonitoringZone zone) { this.zone = zone; }

    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public Double getAvgTemp() { return avgTemp; }
    public void setAvgTemp(Double avgTemp) { this.avgTemp = avgTemp; }

    public Double getAvgRainfall() { return avgRainfall; }
    public void setAvgRainfall(Double avgRainfall) { this.avgRainfall = avgRainfall; }

    public Double getAvgHumidity() { return avgHumidity; }
    public void setAvgHumidity(Double avgHumidity) { this.avgHumidity = avgHumidity; }

    public Double getAvgPressure() { return avgPressure; }
    public void setAvgPressure(Double avgPressure) { this.avgPressure = avgPressure; }

    public Double getAvgWindSpeed() { return avgWindSpeed; }
    public void setAvgWindSpeed(Double avgWindSpeed) { this.avgWindSpeed = avgWindSpeed; }

    public Double getMeanWindDir() { return meanWindDir; }
    public void setMeanWindDir(Double meanWindDir) { this.meanWindDir = meanWindDir; }

    public String getComputedBy() { return computedBy; }
    public void setComputedBy(String computedBy) { this.computedBy = computedBy; }

    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}
