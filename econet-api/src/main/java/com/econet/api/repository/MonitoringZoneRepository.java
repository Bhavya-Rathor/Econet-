package com.econet.api.repository;

import com.econet.api.model.MonitoringZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonitoringZoneRepository extends JpaRepository<MonitoringZone, Long> {
    Optional<MonitoringZone> findFirstByOrderByIdAsc();
}
