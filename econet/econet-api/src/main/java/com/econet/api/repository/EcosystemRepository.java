package com.econet.api.repository;

import com.econet.api.model.Ecosystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EcosystemRepository extends JpaRepository<Ecosystem, Long> {
}
