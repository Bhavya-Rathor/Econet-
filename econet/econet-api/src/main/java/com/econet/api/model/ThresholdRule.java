package com.econet.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "threshold_rules")
public class ThresholdRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String domain; // "AIR", "WATER", "SOIL", "POLLUTION"

    @Column(nullable = false, unique = true, length = 50)
    private String parameterName; // e.g. "AQI", "PM2_5", "WATER_PH", "SOIL_MOISTURE", "NOISE_DB"

    @Column(nullable = false)
    private Double warningLimit;

    @Column(nullable = false)
    private Double criticalLimit;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, length = 255)
    private String actionProtocol; // Productive cloud recommendation

    // If true, a breach occurs when value is BELOW limit (e.g. soil moisture, dissolved oxygen, or low pH)
    @Column(nullable = false)
    private boolean lowerIsCritical = false;

    public ThresholdRule() {}

    public ThresholdRule(String domain, String parameterName, Double warningLimit, Double criticalLimit, String unit, String actionProtocol, boolean lowerIsCritical) {
        this.domain = domain;
        this.parameterName = parameterName;
        this.warningLimit = warningLimit;
        this.criticalLimit = criticalLimit;
        this.unit = unit;
        this.actionProtocol = actionProtocol;
        this.lowerIsCritical = lowerIsCritical;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public Double getWarningLimit() { return warningLimit; }
    public void setWarningLimit(Double warningLimit) { this.warningLimit = warningLimit; }

    public Double getCriticalLimit() { return criticalLimit; }
    public void setCriticalLimit(Double criticalLimit) { this.criticalLimit = criticalLimit; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getActionProtocol() { return actionProtocol; }
    public void setActionProtocol(String actionProtocol) { this.actionProtocol = actionProtocol; }

    public boolean isLowerIsCritical() { return lowerIsCritical; }
    public void setLowerIsCritical(boolean lowerIsCritical) { this.lowerIsCritical = lowerIsCritical; }
}
