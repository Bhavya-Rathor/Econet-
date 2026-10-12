-- =========================================================================
-- EcoNet: Cloud-Native Hyper-Local Environmental Intelligence System
-- DBMS Relational Schema (PostgreSQL 15+, 3NF Normalized)
-- Location: Vellore Smart Eco-Zone (Palar River Basin & Katpadi Corridor), TN
-- =========================================================================

-- 1. Location / Monitoring Station Master Table
CREATE TABLE IF NOT EXISTS monitoring_zones (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    zone_type VARCHAR(50) NOT NULL, -- e.g., 'SMART_CAMPUS', 'URBAN_FOREST', 'INDUSTRIAL_BELT'
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    description VARCHAR(500),
    installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Configurable Threshold Rules Table (Defines automated productive actions)
CREATE TABLE IF NOT EXISTS threshold_rules (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(30) NOT NULL, -- 'AIR', 'WATER', 'SOIL', 'POLLUTION'
    parameter_name VARCHAR(50) UNIQUE NOT NULL, -- 'AQI', 'PM2_5', 'WATER_PH', 'SOIL_MOISTURE', 'NOISE_DB'
    warning_limit NUMERIC(8, 2) NOT NULL,
    critical_limit NUMERIC(8, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    action_protocol VARCHAR(255) NOT NULL, -- The automated cloud recommendation/action
    lower_is_critical BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3. Time-Series Telemetry Table (Partitionable & Indexed)
CREATE TABLE IF NOT EXISTS environmental_readings (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL REFERENCES monitoring_zones(id) ON DELETE CASCADE,
    domain VARCHAR(30) NOT NULL,
    parameter_name VARCHAR(50) NOT NULL,
    reading_value NUMERIC(8, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- 'NORMAL', 'WARNING', 'CRITICAL'
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- High-performance B-Tree composite indexes for time-series analytics
CREATE INDEX IF NOT EXISTS idx_readings_zone_time ON environmental_readings(zone_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_readings_param ON environmental_readings(parameter_name, recorded_at DESC);

-- 4. Incident Alert Logs Table (Tracks threshold breaches & automated actions)
CREATE TABLE IF NOT EXISTS incident_alerts (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL REFERENCES monitoring_zones(id) ON DELETE CASCADE,
    domain VARCHAR(30) NOT NULL,
    parameter_name VARCHAR(50) NOT NULL,
    breach_value NUMERIC(8, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    severity VARCHAR(20) NOT NULL, -- 'WARNING', 'CRITICAL'
    advisory_action VARCHAR(255) NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP
);

-- =========================================================================
-- ADVANCED DBMS FEATURES (For Faculty Viva & Practical Demonstration)
-- =========================================================================

-- DBMS VIEW: Real-time Live Zone Health Summary
CREATE OR REPLACE VIEW v_live_zone_health AS
SELECT 
    z.id AS zone_id,
    z.name AS zone_name,
    z.zone_type,
    COUNT(a.id) FILTER (WHERE a.resolved = FALSE) AS active_alerts_count,
    MAX(r.recorded_at) AS last_telemetry_time
FROM monitoring_zones z
LEFT JOIN environmental_readings r ON z.id = r.zone_id
LEFT JOIN incident_alerts a ON z.id = a.zone_id
GROUP BY z.id, z.name, z.zone_type;

-- DBMS TRIGGER: Auto-trigger incident alerts if raw SQL inserts violate critical thresholds
CREATE OR REPLACE FUNCTION trg_fn_auto_alert()
RETURNS TRIGGER AS $$
DECLARE
    rule_record RECORD;
    is_breach BOOLEAN := FALSE;
BEGIN
    SELECT * INTO rule_record
    FROM threshold_rules
    WHERE parameter_name = NEW.parameter_name;

    IF FOUND THEN
        IF rule_record.lower_is_critical THEN
            is_breach := (NEW.reading_value <= rule_record.critical_limit);
        ELSE
            is_breach := (NEW.reading_value >= rule_record.critical_limit);
        END IF;

        IF is_breach THEN
            NEW.status := 'CRITICAL';
            INSERT INTO incident_alerts (zone_id, domain, parameter_name, breach_value, unit, severity, advisory_action, triggered_at, resolved)
            VALUES (NEW.zone_id, NEW.domain, NEW.parameter_name, NEW.reading_value, NEW.unit, 'CRITICAL', rule_record.action_protocol, NEW.recorded_at, FALSE);
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_threshold ON environmental_readings;
CREATE TRIGGER trg_check_threshold
BEFORE INSERT ON environmental_readings
FOR EACH ROW
EXECUTE FUNCTION trg_fn_auto_alert();

-- =========================================================================
-- SAMPLE ANALYTICAL SQL QUERIES (Showcase in Project Report & Presentation)
-- =========================================================================

-- Query 1: 24-Hour Statistical Summary (AVG, MIN, MAX) for all parameters
-- SELECT parameter_name, 
--        ROUND(AVG(reading_value), 2) AS avg_reading,
--        MIN(reading_value) AS min_reading,
--        MAX(reading_value) AS max_reading,
--        COUNT(*) AS total_samples
-- FROM environmental_readings
-- WHERE recorded_at >= NOW() - INTERVAL '24 HOURS'
-- GROUP BY parameter_name;

-- Query 2: Peak Pollution / Smog Hours Identification
-- SELECT DATE_TRUNC('hour', recorded_at) AS hour_window,
--        AVG(reading_value) AS hourly_aqi
-- FROM environmental_readings
-- WHERE parameter_name = 'AQI'
-- GROUP BY hour_window
-- HAVING AVG(reading_value) > 100
-- ORDER BY hourly_aqi DESC;
