package com.econet.api.repository;

import com.econet.api.model.IncidentAlert;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentAlertRepository extends JpaRepository<IncidentAlert, Long> {

    List<IncidentAlert> findByZoneIdAndResolvedFalseOrderByTriggeredAtDesc(Long zoneId);

    List<IncidentAlert> findByZoneIdOrderByTriggeredAtDesc(Long zoneId, Pageable pageable);

    long countByZoneIdAndResolvedFalse(Long zoneId);

    List<IncidentAlert> findByResolvedFalseOrderByTriggeredAtDesc();

    // The currently open incident (if any) for one parameter in a zone
    Optional<IncidentAlert> findFirstByZoneIdAndParameterNameAndResolvedFalseOrderByTriggeredAtAsc(Long zoneId, String parameterName);
}
