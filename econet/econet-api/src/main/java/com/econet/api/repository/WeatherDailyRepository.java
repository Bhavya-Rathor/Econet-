package com.econet.api.repository;

import com.econet.api.model.WeatherDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherDailyRepository extends JpaRepository<WeatherDaily, Long> {

    Optional<WeatherDaily> findByZoneIdAndObsDate(Long zoneId, LocalDate obsDate);

    List<WeatherDaily> findByZoneIdAndObsDateBetweenOrderByObsDateAsc(Long zoneId, LocalDate start, LocalDate end);

    // DBMS cross-check of the Hadoop job: the same monthly + overall averages computed in one SQL
    // statement. ROLLUP adds a grand-total row (NULL month -> 'OVERALL'); wind direction uses a
    // circular mean from summed sin/cos, exactly like the MapReduce reducer.
    @Query(value = "SELECT COALESCE(TO_CHAR(obs_date, 'YYYY-MM'), 'OVERALL') AS period_key, " +
                   "       COUNT(*) AS days, " +
                   "       AVG(temp_mean) AS avg_temp, " +
                   "       AVG(rainfall_mm) AS avg_rainfall, " +
                   "       AVG(humidity_pct) AS avg_humidity, " +
                   "       AVG(pressure_hpa) AS avg_pressure, " +
                   "       AVG(wind_speed_kmh) AS avg_wind_speed, " +
                   "       MOD(CAST(DEGREES(ATAN2(SUM(SIN(RADIANS(wind_dir_deg))), SUM(COS(RADIANS(wind_dir_deg))))) + 360 AS NUMERIC), 360) AS mean_wind_dir " +
                   "FROM weather_daily " +
                   "WHERE zone_id = :zoneId AND obs_date BETWEEN :start AND :end " +
                   "GROUP BY ROLLUP (TO_CHAR(obs_date, 'YYYY-MM')) " +
                   "ORDER BY period_key",
           nativeQuery = true)
    List<Object[]> aggregateByMonthWithRollup(@Param("zoneId") Long zoneId,
                                              @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);
}
