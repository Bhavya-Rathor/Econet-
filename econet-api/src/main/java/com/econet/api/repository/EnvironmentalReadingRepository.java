package com.econet.api.repository;

import com.econet.api.model.EnvironmentalReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface EnvironmentalReadingRepository extends JpaRepository<EnvironmentalReading, Long> {

    List<EnvironmentalReading> findByZoneIdOrderByRecordedAtDesc(Long zoneId, Pageable pageable);

    List<EnvironmentalReading> findByZoneIdAndDomainOrderByRecordedAtDesc(Long zoneId, String domain, Pageable pageable);

    List<EnvironmentalReading> findByZoneIdAndParameterNameOrderByRecordedAtDesc(Long zoneId, String parameterName, Pageable pageable);

    // Analytical query for DBMS: Latest reading per parameter for a zone
    @Query(value = "SELECT DISTINCT ON (parameter_name) * FROM environmental_readings WHERE zone_id = :zoneId ORDER BY parameter_name, recorded_at DESC", nativeQuery = true)
    List<EnvironmentalReading> findLatestReadingsByZone(@Param("zoneId") Long zoneId);

    // Analytical aggregation query: 24-hour moving statistical averages
    @Query("SELECT r.parameterName AS param, AVG(r.readingValue) AS avgVal, MIN(r.readingValue) AS minVal, MAX(r.readingValue) AS maxVal, COUNT(r) AS totalSamples " +
           "FROM EnvironmentalReading r " +
           "WHERE r.zone.id = :zoneId AND r.recordedAt >= :since " +
           "GROUP BY r.parameterName")
    List<Object[]> get24HourStatistics(@Param("zoneId") Long zoneId, @Param("since") LocalDateTime since);

    // Count readings by status
    long countByZoneIdAndStatus(Long zoneId, String status);
}
