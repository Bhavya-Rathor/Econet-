package com.econet.api.repository;

import com.econet.api.model.SuccessionStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuccessionStageRepository extends JpaRepository<SuccessionStage, Long> {
    List<SuccessionStage> findByEcosystemIdOrderByStageOrderAsc(Long ecosystemId);
}
