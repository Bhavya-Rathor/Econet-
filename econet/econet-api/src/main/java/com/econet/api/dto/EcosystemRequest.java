package com.econet.api.dto;

public class EcosystemRequest {
    private String name;
    private String description;
    private String location;
    private Double areaInHectares;
    private String status; // Must match the JSON key sent from frontend

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Double getAreaInHectares() { return areaInHectares; }
    public String getStatus() { return status; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setAreaInHectares(Double areaInHectares) { this.areaInHectares = areaInHectares; }
    public void setStatus(String status) { this.status = status; }
}