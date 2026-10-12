package com.econet.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ecosystems")
public class Ecosystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String location;
    private Double areaInHectares;
    
    @Column(nullable = false)
    private String status = "ACTIVE"; // Default value

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Double getAreaInHectares() { return areaInHectares; }
    public String getStatus() { return status; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setAreaInHectares(Double areaInHectares) { this.areaInHectares = areaInHectares; }
    public void setStatus(String status) { this.status = status; }
}