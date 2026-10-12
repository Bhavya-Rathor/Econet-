package com.econet.api.repository;

import com.econet.api.model.ThresholdRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ThresholdRuleRepository extends JpaRepository<ThresholdRule, Long> {
    Optional<ThresholdRule> findByParameterName(String parameterName);
    List<ThresholdRule> findByDomain(String domain);
}
