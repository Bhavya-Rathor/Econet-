package com.econet.api.service;

import com.econet.api.dto.EcosystemRequest;
import com.econet.api.model.Ecosystem;
import com.econet.api.model.SuccessionStage;
import com.econet.api.repository.EcosystemRepository;
import com.econet.api.repository.SuccessionStageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EcologyService {

    @Autowired
    private EcosystemRepository ecosystemRepository;

    @Autowired
    private SuccessionStageRepository successionStageRepository;

    public List<Ecosystem> getAllEcosystems() {
        return ecosystemRepository.findAll();
    }

    public Ecosystem getEcosystemById(Long id) {
        return ecosystemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ecosystem not found with id: " + id));
    }

    public Ecosystem createEcosystem(EcosystemRequest request) {
        Ecosystem ecosystem = new Ecosystem();
        ecosystem.setName(request.getName());
        ecosystem.setDescription(request.getDescription());
        ecosystem.setLocation(request.getLocation());
        ecosystem.setAreaInHectares(request.getAreaInHectares());
        
        // Handle status as a direct String string
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            ecosystem.setStatus(request.getStatus().toUpperCase());
        } else {
            ecosystem.setStatus("ACTIVE");
        }
        
        return ecosystemRepository.save(ecosystem);
    }

    public Ecosystem updateEcosystem(Long id, EcosystemRequest request) {
        Ecosystem ecosystem = getEcosystemById(id);
        ecosystem.setName(request.getName());
        ecosystem.setDescription(request.getDescription());
        ecosystem.setLocation(request.getLocation());
        ecosystem.setAreaInHectares(request.getAreaInHectares());
        
        // Update status directly as a String
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            ecosystem.setStatus(request.getStatus().toUpperCase());
        }
        
        return ecosystemRepository.save(ecosystem);
    }

    public void deleteEcosystem(Long id) {
        ecosystemRepository.deleteById(id);
    }

    public List<SuccessionStage> getSuccessionTimeline(Long ecosystemId) {
        return successionStageRepository.findByEcosystemIdOrderByStageOrderAsc(ecosystemId);
    }
}