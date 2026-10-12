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

-- Partial unique index: at most ONE unresolved alert per zone + parameter.
-- Resolved alerts are not indexed, so history can hold any number of past incidents.
CREATE UNIQUE INDEX IF NOT EXISTS uq_one_open_alert_per_parameter
    ON incident_alerts (zone_id, parameter_name)
    WHERE resolved = FALSE;

-- =========================================================================
-- ADVANCED DBMS FEATURES (For Faculty Viva & Practical Demonstration)
-- =========================================================================

-- DBMS VIEW: Real-time Live Zone Health Summary
-- Correlated subqueries (not two LEFT JOINs): joining readings AND alerts to the zone at once
-- pairs every reading with every alert, so COUNT would multiply (join fan-out).
CREATE OR REPLACE VIEW v_live_zone_health AS
SELECT
    z.id AS zone_id,
    z.name AS zone_name,
    z.zone_type,
    (SELECT COUNT(*) FROM incident_alerts a
      WHERE a.zone_id = z.id AND a.resolved = FALSE) AS active_alerts_count,
    (SELECT MAX(r.recorded_at) FROM environmental_readings r
      WHERE r.zone_id = z.id) AS last_telemetry_time
FROM monitoring_zones z;

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
            -- One open incident per zone + parameter: skip if this breach is already being tracked
            IF NOT EXISTS (SELECT 1 FROM incident_alerts
                           WHERE zone_id = NEW.zone_id AND parameter_name = NEW.parameter_name AND resolved = FALSE) THEN
                INSERT INTO incident_alerts (zone_id, domain, parameter_name, breach_value, unit, severity, advisory_action, triggered_at, resolved)
                VALUES (NEW.zone_id, NEW.domain, NEW.parameter_name, NEW.reading_value, NEW.unit, 'CRITICAL', rule_record.action_protocol, NEW.recorded_at, FALSE);
            END IF;
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

-- =========================================================================
-- WEATHER ANALYTICS MODULE (PostgreSQL <-> Hadoop MapReduce pipeline)
-- Flow: Open-Meteo archive -> weather_daily -> export .dat -> Hadoop MapReduce
--       -> part-r-00000 -> weather_monthly_summary -> SQL ROLLUP cross-check
-- =========================================================================

-- 5. Daily observed weather (real data from the Open-Meteo historical archive)
CREATE TABLE IF NOT EXISTS weather_daily (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL REFERENCES monitoring_zones(id) ON DELETE CASCADE,
    obs_date DATE NOT NULL,
    temp_mean DOUBLE PRECISION NOT NULL,            -- deg C
    rainfall_mm DOUBLE PRECISION NOT NULL CHECK (rainfall_mm >= 0),
    humidity_pct DOUBLE PRECISION NOT NULL CHECK (humidity_pct BETWEEN 0 AND 100),
    pressure_hpa DOUBLE PRECISION NOT NULL,         -- mean sea level
    wind_speed_kmh DOUBLE PRECISION NOT NULL CHECK (wind_speed_kmh >= 0),
    wind_dir_deg DOUBLE PRECISION NOT NULL CHECK (wind_dir_deg >= 0 AND wind_dir_deg <= 360),
    source VARCHAR(30) NOT NULL DEFAULT 'OPEN_METEO_ARCHIVE',
    ingested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weather_daily_zone_date UNIQUE (zone_id, obs_date)
);
CREATE INDEX IF NOT EXISTS idx_weather_daily_date ON weather_daily(obs_date);

-- 6. Per-month (and OVERALL) averages written back from the Hadoop MapReduce output
CREATE TABLE IF NOT EXISTS weather_monthly_summary (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL REFERENCES monitoring_zones(id) ON DELETE CASCADE,
    period_key VARCHAR(20) NOT NULL,                -- '2026-04' ... or 'OVERALL'
    days INTEGER NOT NULL CHECK (days > 0),
    avg_temp DOUBLE PRECISION NOT NULL,
    avg_rainfall DOUBLE PRECISION NOT NULL,
    avg_humidity DOUBLE PRECISION NOT NULL,
    avg_pressure DOUBLE PRECISION NOT NULL,
    avg_wind_speed DOUBLE PRECISION NOT NULL,
    mean_wind_dir DOUBLE PRECISION NOT NULL,        -- circular mean
    computed_by VARCHAR(30) NOT NULL DEFAULT 'HADOOP_MAPREDUCE',
    computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weather_summary_zone_period_src UNIQUE (zone_id, period_key, computed_by)
);

-- 7. Daily forecasts (several models can be stored side by side)
CREATE TABLE IF NOT EXISTS weather_forecast (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL REFERENCES monitoring_zones(id) ON DELETE CASCADE,
    forecast_date DATE NOT NULL,
    pred_temp DOUBLE PRECISION NOT NULL,
    pred_rainfall DOUBLE PRECISION NOT NULL,
    pred_humidity DOUBLE PRECISION NOT NULL,
    pred_pressure DOUBLE PRECISION NOT NULL,
    pred_wind_speed DOUBLE PRECISION NOT NULL,
    pred_wind_dir DOUBLE PRECISION NOT NULL,
    model_name VARCHAR(50) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_weather_forecast_zone_date_model UNIQUE (zone_id, forecast_date, model_name)
);

-- DBMS VIEW: the MapReduce aggregation expressed in SQL.
-- ROLLUP adds the grand-total row; wind direction uses a circular mean (sum of sin/cos).
CREATE OR REPLACE VIEW v_weather_monthly_sql AS
SELECT zone_id,
       COALESCE(TO_CHAR(obs_date, 'YYYY-MM'), 'OVERALL') AS period_key,
       COUNT(*) AS days,
       ROUND(AVG(temp_mean)::NUMERIC, 2) AS avg_temp,
       ROUND(AVG(rainfall_mm)::NUMERIC, 2) AS avg_rainfall,
       ROUND(AVG(humidity_pct)::NUMERIC, 2) AS avg_humidity,
       ROUND(AVG(pressure_hpa)::NUMERIC, 2) AS avg_pressure,
       ROUND(AVG(wind_speed_kmh)::NUMERIC, 2) AS avg_wind_speed,
       ROUND(MOD((DEGREES(ATAN2(SUM(SIN(RADIANS(wind_dir_deg))), SUM(COS(RADIANS(wind_dir_deg))))) + 360)::NUMERIC, 360), 2) AS mean_wind_dir
FROM weather_daily
GROUP BY zone_id, ROLLUP (TO_CHAR(obs_date, 'YYYY-MM'));

-- Query 3: Side-by-side Hadoop vs SQL results (JOIN on period) - differences should be ~0
-- SELECT h.period_key, h.avg_temp AS hadoop_temp, s.avg_temp AS sql_temp,
--        ABS(h.avg_temp - s.avg_temp) AS temp_diff,
--        h.mean_wind_dir AS hadoop_wind_dir, s.mean_wind_dir AS sql_wind_dir
-- FROM weather_monthly_summary h
-- JOIN v_weather_monthly_sql s ON s.zone_id = h.zone_id AND s.period_key = h.period_key
-- WHERE h.computed_by = 'HADOOP_MAPREDUCE'
-- ORDER BY h.period_key;

-- Query 4: Wettest days per month using a window function
-- SELECT * FROM (
--     SELECT obs_date, rainfall_mm,
--            RANK() OVER (PARTITION BY DATE_TRUNC('month', obs_date) ORDER BY rainfall_mm DESC) AS rank_in_month
--     FROM weather_daily
-- ) t
-- WHERE rank_in_month <= 3 AND rainfall_mm > 0
-- ORDER BY obs_date;
