package com.econet.api.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily predicted weather produced by a forecasting model.
 * modelName lets several models' predictions be stored and compared side by side.
 */
@Entity
@Table(name = "weather_forecast",
       uniqueConstraints = @UniqueConstraint(name = "uq_weather_forecast_zone_date_model",
                                             columnNames = {"zone_id", "forecast_date", "model_name"}))
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private MonitoringZone zone;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(nullable = false)
    private Double predTemp;

    @Column(nullable = false)
    private Double predRainfall;

    @Column(nullable = false)
    private Double predHumidity;

    @Column(nullable = false)
    private Double predPressure;

    @Column(nullable = false)
    private Double predWindSpeed;

    @Column(nullable = false)
    private Double predWindDir;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    public WeatherForecast() {}

    public WeatherForecast(MonitoringZone zone, LocalDate forecastDate, String modelName) {
        this.zone = zone;
        this.forecastDate = forecastDate;
        this.modelName = modelName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MonitoringZone getZone() { return zone; }
    public void setZone(MonitoringZone zone) { this.zone = zone; }

    public LocalDate getForecastDate() { return forecastDate; }
    public void setForecastDate(LocalDate forecastDate) { this.forecastDate = forecastDate; }

    public Double getPredTemp() { return predTemp; }
    public void setPredTemp(Double predTemp) { this.predTemp = predTemp; }

    public Double getPredRainfall() { return predRainfall; }
    public void setPredRainfall(Double predRainfall) { this.predRainfall = predRainfall; }

    public Double getPredHumidity() { return predHumidity; }
    public void setPredHumidity(Double predHumidity) { this.predHumidity = predHumidity; }

    public Double getPredPressure() { return predPressure; }
    public void setPredPressure(Double predPressure) { this.predPressure = predPressure; }

    public Double getPredWindSpeed() { return predWindSpeed; }
    public void setPredWindSpeed(Double predWindSpeed) { this.predWindSpeed = predWindSpeed; }

    public Double getPredWindDir() { return predWindDir; }
    public void setPredWindDir(Double predWindDir) { this.predWindDir = predWindDir; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
