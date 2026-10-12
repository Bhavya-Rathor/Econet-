package com.econet.api.repository;

import com.econet.api.model.WeatherMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherMonthlySummaryRepository extends JpaRepository<WeatherMonthlySummary, Long> {

    Optional<WeatherMonthlySummary> findByZoneIdAndPeriodKeyAndComputedBy(Long zoneId, String periodKey, String computedBy);

    List<WeatherMonthlySummary> findByZoneIdAndComputedByOrderByPeriodKeyAsc(Long zoneId, String computedBy);
}
