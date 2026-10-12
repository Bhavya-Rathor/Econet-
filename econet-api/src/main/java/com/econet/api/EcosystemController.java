package com.econet.api.controller;

import com.econet.api.dto.EcosystemRequest;
import com.econet.api.model.Ecosystem;
import com.econet.api.model.SuccessionStage;
import com.econet.api.service.EcologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ecosystems")
@CrossOrigin(origins = "*") // Allows your frontend to talk to the backend without CORS issues
public class EcosystemController {

    @Autowired
    private EcologyService ecologyService;

    @GetMapping
    public ResponseEntity<List<Ecosystem>> getAllEcosystems() {
        return ResponseEntity.ok(ecologyService.getAllEcosystems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ecosystem> getEcosystemById(@PathVariable Long id) {
        return ResponseEntity.ok(ecologyService.getEcosystemById(id));
    }

    @PostMapping
    public ResponseEntity<Ecosystem> createEcosystem(@RequestBody EcosystemRequest request) {
        Ecosystem created = ecologyService.createEcosystem(request);
        return ResponseEntity.ok(created);
    }

    // --- THIS IS THE CRITICAL UPDATE ROUTE ---
    @PutMapping("/{id}")
    public ResponseEntity<Ecosystem> updateEcosystem(@PathVariable Long id, @RequestBody EcosystemRequest request) {
        Ecosystem updated = ecologyService.updateEcosystem(id, request);
        return ResponseEntity.ok(updated);
    }
    // ----------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEcosystem(@PathVariable Long id) {
        ecologyService.deleteEcosystem(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<SuccessionStage>> getSuccessionTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(ecologyService.getSuccessionTimeline(id));
    }
}