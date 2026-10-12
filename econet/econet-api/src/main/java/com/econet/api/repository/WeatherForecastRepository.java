package com.econet.api.repository;

import com.econet.api.model.WeatherForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, Long> {

    Optional<WeatherForecast> findByZoneIdAndForecastDateAndModelName(Long zoneId, LocalDate forecastDate, String modelName);

    List<WeatherForecast> findByZoneIdAndModelNameOrderByForecastDateAsc(Long zoneId, String modelName);
}
