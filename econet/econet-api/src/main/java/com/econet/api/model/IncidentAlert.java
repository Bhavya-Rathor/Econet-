package com.econet.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_alerts")
public class IncidentAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "zone_id", nullable = false)
    private MonitoringZone zone;

    @Column(nullable = false, length = 30)
    private String domain;

    @Column(nullable = false, length = 50)
    private String parameterName;

    @Column(nullable = false)
    private Double breachValue;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, length = 20)
    private String severity; // "WARNING", "CRITICAL"

    @Column(nullable = false, length = 255)
    private String advisoryAction; // The automated recommendation

    @Column(nullable = false)
    private LocalDateTime triggeredAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean resolved = false;

    private LocalDateTime resolvedAt;

    public IncidentAlert() {}

    public IncidentAlert(MonitoringZone zone, String domain, String parameterName, Double breachValue, String unit, String severity, String advisoryAction) {
        this.zone = zone;
        this.domain = domain;
        this.parameterName = parameterName;
        this.breachValue = breachValue;
        this.unit = unit;
        this.severity = severity;
        this.advisoryAction = advisoryAction;
        this.triggeredAt = LocalDateTime.now();
        this.resolved = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MonitoringZone getZone() { return zone; }
    public void setZone(MonitoringZone zone) { this.zone = zone; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }

    public Double getBreachValue() { return breachValue; }
    public void setBreachValue(Double breachValue) { this.breachValue = breachValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getAdvisoryAction() { return advisoryAction; }
    public void setAdvisoryAction(String advisoryAction) { this.advisoryAction = advisoryAction; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
