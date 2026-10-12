package com.econet.api.service;

import com.econet.api.model.MonitoringZone;
import com.econet.api.model.WeatherDaily;
import com.econet.api.model.WeatherForecast;
import com.econet.api.model.WeatherMonthlySummary;
import com.econet.api.repository.WeatherDailyRepository;
import com.econet.api.repository.WeatherForecastRepository;
import com.econet.api.repository.WeatherMonthlySummaryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Cloud data pipeline linking EcoNet's PostgreSQL database with the Hadoop MapReduce job:
 *
 *   Open-Meteo archive --backfill--> weather_daily --export .dat--> Hadoop MapReduce
 *        --part-r-00000 import--> weather_monthly_summary --verify--> SQL ROLLUP cross-check
 */
@Service
public class WeatherPipelineService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherPipelineService.class);

    public static final String HADOOP_SOURCE = "HADOOP_MAPREDUCE";
    private static final String DAT_HEADER = "date,temp,rainfall,humidity,pressure,wind_speed,wind_direction";
    private static final double VERIFY_TOLERANCE = 0.011; // Hadoop output is rounded to 2 decimals

    @Autowired
    private EnvironmentalTelemetryService telemetryService;

    @Autowired
    private WeatherDailyRepository dailyRepository;

    @Autowired
    private WeatherMonthlySummaryRepository summaryRepository;

    @Autowired
    private WeatherForecastRepository forecastRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ------------------------------------------------------------------
    // 1. INGEST: Open-Meteo historical archive -> weather_daily
    // ------------------------------------------------------------------
    @Transactional
    public Map<String, Object> backfillFromOpenMeteo(LocalDate start, LocalDate end) throws Exception {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end must be on or after start");
        }
        MonitoringZone zone = telemetryService.getDefaultZone();

        String url = String.format(Locale.ROOT,
                "https://archive-api.open-meteo.com/v1/archive?latitude=%.4f&longitude=%.4f&start_date=%s&end_date=%s" +
                "&daily=temperature_2m_mean,precipitation_sum,relative_humidity_2m_mean,pressure_msl_mean," +
                "wind_speed_10m_mean,wind_direction_10m_dominant&timezone=Asia%%2FKolkata",
                zone.getLatitude(), zone.getLongitude(), start, end);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Open-Meteo returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode daily = objectMapper.readTree(response.body()).path("daily");
        JsonNode dates = daily.path("time");
        int inserted = 0, updated = 0, skipped = 0;

        for (int i = 0; i < dates.size(); i++) {
            JsonNode temp = daily.path("temperature_2m_mean").get(i);
            JsonNode rain = daily.path("precipitation_sum").get(i);
            JsonNode hum = daily.path("relative_humidity_2m_mean").get(i);
            JsonNode pres = daily.path("pressure_msl_mean").get(i);
            JsonNode ws = daily.path("wind_speed_10m_mean").get(i);
            JsonNode wd = daily.path("wind_direction_10m_dominant").get(i);
            if (isMissing(temp) || isMissing(rain) || isMissing(hum) || isMissing(pres) || isMissing(ws) || isMissing(wd)) {
                skipped++; // archive has not published this day yet
                continue;
            }

            LocalDate date = LocalDate.parse(dates.get(i).asText());
            Optional<WeatherDaily> existing = dailyRepository.findByZoneIdAndObsDate(zone.getId(), date);
            WeatherDaily row = existing.orElseGet(() -> new WeatherDaily(zone, date));
            row.setTempMean(temp.asDouble());
            row.setRainfallMm(rain.asDouble());
            row.setHumidityPct(hum.asDouble());
            row.setPressureHpa(pres.asDouble());
            row.setWindSpeedKmh(ws.asDouble());
            row.setWindDirDeg(wd.asDouble());
            row.setIngestedAt(LocalDateTime.now());
            dailyRepository.save(row);

            if (existing.isPresent()) updated++; else inserted++;
        }

        logger.info("[WEATHER PIPELINE] Backfill {}..{}: inserted={}, updated={}, skipped={}", start, end, inserted, updated, skipped);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("zone", zone.getName());
        result.put("start", start);
        result.put("end", end);
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("skippedMissing", skipped);
        return result;
    }

    private static boolean isMissing(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode();
    }

    // ------------------------------------------------------------------
    // 2. EXPORT: weather_daily -> .dat file for HDFS / MapReduce input
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public String exportDat(LocalDate start, LocalDate end) {
        MonitoringZone zone = telemetryService.getDefaultZone();
        StringBuilder sb = new StringBuilder(DAT_HEADER).append('\n');
        for (WeatherDaily d : dailyRepository.findByZoneIdAndObsDateBetweenOrderByObsDateAsc(zone.getId(), start, end)) {
            sb.append(d.getObsDate()).append(',')
              .append(d.getTempMean()).append(',')
              .append(d.getRainfallMm()).append(',')
              .append(d.getHumidityPct()).append(',')
              .append(d.getPressureHpa()).append(',')
              .append(d.getWindSpeedKmh()).append(',')
              .append(d.getWindDirDeg()).append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 3. IMPORT: Hadoop part-r-00000 -> weather_monthly_summary
    //    Line format: MONTH_2026-04<TAB>days=30,temp=31.64,rainfall=0.03,...,wind_dir=144.23
    // ------------------------------------------------------------------
    @Transactional
    public List<WeatherMonthlySummary> importMapReduceOutput(String partFileContent) {
        MonitoringZone zone = telemetryService.getDefaultZone();
        List<WeatherMonthlySummary> saved = new ArrayList<>();

        for (String rawLine : partFileContent.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            String[] keyValue = line.split("\t", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Not a MapReduce output line (expected KEY<TAB>VALUES): " + line);
            }
            String periodKey = keyValue[0].startsWith("MONTH_") ? keyValue[0].substring("MONTH_".length()) : keyValue[0];

            Map<String, Double> fields = new HashMap<>();
            for (String pair : keyValue[1].split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) fields.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
            }
            for (String required : List.of("days", "temp", "rainfall", "humidity", "pressure", "wind_speed", "wind_dir")) {
                if (!fields.containsKey(required)) {
                    throw new IllegalArgumentException("Missing '" + required + "' in line: " + line);
                }
            }

            WeatherMonthlySummary s = summaryRepository
                    .findByZoneIdAndPeriodKeyAndComputedBy(zone.getId(), periodKey, HADOOP_SOURCE)
                    .orElseGet(() -> new WeatherMonthlySummary(zone, periodKey, HADOOP_SOURCE));
            s.setDays(fields.get("days").intValue());
            s.setAvgTemp(fields.get("temp"));
            s.setAvgRainfall(fields.get("rainfall"));
            s.setAvgHumidity(fields.get("humidity"));
            s.setAvgPressure(fields.get("pressure"));
            s.setAvgWindSpeed(fields.get("wind_speed"));
            s.setMeanWindDir(fields.get("wind_dir"));
            s.setComputedAt(LocalDateTime.now());
            saved.add(summaryRepository.save(s));
        }

        logger.info("[WEATHER PIPELINE] Imported {} MapReduce summary rows", saved.size());
        return saved;
    }

    // ------------------------------------------------------------------
    // 5. FORECAST: train_forecast.py CSV -> weather_forecast
    //    Header: date,temp,rainfall,humidity,pressure,wind_speed,wind_direction
    // ------------------------------------------------------------------
    @Transactional
    public int importForecastCsv(String csv, String modelName) {
        MonitoringZone zone = telemetryService.getDefaultZone();
        String[] lines = csv.split("\\R");
        if (lines.length < 2 || !lines[0].trim().equals(DAT_HEADER)) {
            throw new IllegalArgumentException("Expected CSV header: " + DAT_HEADER);
        }

        int saved = 0;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] t = line.split(",");
            if (t.length != 7) {
                throw new IllegalArgumentException("Line " + (i + 1) + " should have 7 values: " + line);
            }

            LocalDate date = LocalDate.parse(t[0]);
            WeatherForecast f = forecastRepository
                    .findByZoneIdAndForecastDateAndModelName(zone.getId(), date, modelName)
                    .orElseGet(() -> new WeatherForecast(zone, date, modelName));
            f.setPredTemp(Double.parseDouble(t[1]));
            f.setPredRainfall(Double.parseDouble(t[2]));
            f.setPredHumidity(Double.parseDouble(t[3]));
            f.setPredPressure(Double.parseDouble(t[4]));
            f.setPredWindSpeed(Double.parseDouble(t[5]));
            f.setPredWindDir(Double.parseDouble(t[6]));
            f.setGeneratedAt(LocalDateTime.now());
            forecastRepository.save(f);
            saved++;
        }

        logger.info("[WEATHER PIPELINE] Imported {} forecast days for model {}", saved, modelName);
        return saved;
    }
        @Transactional(readOnly = true)
    public List<Map<String, Object>> getDaily(LocalDate start, LocalDate end) {
        MonitoringZone zone = telemetryService.getDefaultZone();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WeatherDaily d : dailyRepository.findByZoneIdAndObsDateBetweenOrderByObsDateAsc(zone.getId(), start, end)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.getObsDate());
            m.put("temp", d.getTempMean());
            m.put("rainfall", d.getRainfallMm());
            m.put("humidity", d.getHumidityPct());
            m.put("pressure", d.getPressureHpa());
            m.put("windSpeed", d.getWindSpeedKmh());
            m.put("windDir", d.getWindDirDeg());
            rows.add(m);
        }
        return rows;
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getForecast(String modelName) {
        MonitoringZone zone = telemetryService.getDefaultZone();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WeatherForecast f : forecastRepository.findByZoneIdAndModelNameOrderByForecastDateAsc(zone.getId(), modelName)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", f.getForecastDate());
            m.put("temp", f.getPredTemp());
            m.put("rainfall", f.getPredRainfall());
            m.put("humidity", f.getPredHumidity());
            m.put("pressure", f.getPredPressure());
            m.put("windSpeed", f.getPredWindSpeed());
            m.put("windDir", f.getPredWindDir());
            m.put("modelName", f.getModelName());
            rows.add(m);
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHadoopSummaries() {
        MonitoringZone zone = telemetryService.getDefaultZone();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (WeatherMonthlySummary s : summaryRepository.findByZoneIdAndComputedByOrderByPeriodKeyAsc(zone.getId(), HADOOP_SOURCE)) {
            rows.add(summaryToMap(s));
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // 4. VERIFY: Hadoop results vs. the same aggregation done in PostgreSQL
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Map<String, Object> verifyAgainstSql(LocalDate start, LocalDate end) {
        MonitoringZone zone = telemetryService.getDefaultZone();

        Map<String, WeatherMonthlySummary> hadoop = new HashMap<>();
        for (WeatherMonthlySummary s : summaryRepository.findByZoneIdAndComputedByOrderByPeriodKeyAsc(zone.getId(), HADOOP_SOURCE)) {
            hadoop.put(s.getPeriodKey(), s);
        }

        List<Map<String, Object>> comparisons = new ArrayList<>();
        boolean allMatch = !hadoop.isEmpty();

        for (Object[] r : dailyRepository.aggregateByMonthWithRollup(zone.getId(), start, end)) {
            String key = (String) r[0];
            double[] sql = {
                    ((Number) r[1]).doubleValue(), ((Number) r[2]).doubleValue(), ((Number) r[3]).doubleValue(),
                    ((Number) r[4]).doubleValue(), ((Number) r[5]).doubleValue(), ((Number) r[6]).doubleValue(),
                    ((Number) r[7]).doubleValue()
            };

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("periodKey", key);
            row.put("sql", Map.of(
                    "days", (long) sql[0], "avgTemp", round2(sql[1]), "avgRainfall", round2(sql[2]),
                    "avgHumidity", round2(sql[3]), "avgPressure", round2(sql[4]),
                    "avgWindSpeed", round2(sql[5]), "meanWindDir", round2(sql[6])));

            WeatherMonthlySummary h = hadoop.get(key);
            if (h == null) {
                row.put("hadoop", null);
                row.put("match", false);
                allMatch = false;
            } else {
                double[] hv = {h.getDays(), h.getAvgTemp(), h.getAvgRainfall(), h.getAvgHumidity(),
                               h.getAvgPressure(), h.getAvgWindSpeed(), h.getMeanWindDir()};
                double maxDiff = 0;
                for (int i = 0; i < hv.length; i++) {
                    double diff = Math.abs(hv[i] - sql[i]);
                    if (i == 6) diff = Math.min(diff, 360 - diff); // 359.99 deg vs 0.00 deg is a match
                    maxDiff = Math.max(maxDiff, diff);
                }
                boolean match = maxDiff <= VERIFY_TOLERANCE;
                row.put("hadoop", summaryToMap(h));
                row.put("maxAbsDifference", round2(maxDiff));
                row.put("match", match);
                allMatch &= match;
            }
            comparisons.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("start", start);
        result.put("end", end);
        result.put("allMatch", allMatch && !comparisons.isEmpty());
        result.put("comparisons", comparisons);
        return result;
    }

    private static Map<String, Object> summaryToMap(WeatherMonthlySummary s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("periodKey", s.getPeriodKey());
        m.put("days", s.getDays());
        m.put("avgTemp", s.getAvgTemp());
        m.put("avgRainfall", s.getAvgRainfall());
        m.put("avgHumidity", s.getAvgHumidity());
        m.put("avgPressure", s.getAvgPressure());
        m.put("avgWindSpeed", s.getAvgWindSpeed());
        m.put("meanWindDir", s.getMeanWindDir());
        m.put("computedBy", s.getComputedBy());
        m.put("computedAt", s.getComputedAt());
        return m;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
