# Weather Analytics: Hadoop MapReduce over EcoNet's PostgreSQL data

1. **Real 6-month daily dataset**: Vellore (EcoNet's monitoring zone), 1 Apr – 30 Sep 2026, from the
   [Open-Meteo Historical Weather API](https://open-meteo.com/en/docs/historical-weather-api) (ERA5 reanalysis).
   Attributes: `date, temp, rainfall, humidity, pressure, wind_speed, wind_direction`.
2. **Hadoop MapReduce (Java)**: monthly and overall averages of every attribute, with a Combiner and a
   correct circular mean for wind direction.
3. **PostgreSQL integration**: data is loaded into EcoNet's database, exported to the MapReduce job, and the
   results are written back and cross-checked against the same aggregation in SQL.
4. **3-month forecast** (Oct–Dec): `WeatherPredictor.java` / `ml_predict.py` (rule-based, to be replaced by a
   model trained on the data).

---

## Pipeline

```text
Open-Meteo archive ──backfill──▶ PostgreSQL weather_daily
                                        │  GET /api/weather/pipeline/export
                                        ▼
                               data/weather_data.dat ──▶ HDFS ──▶ MapReduce (Mapper → Combiner → Reducer)
                                                                         │ part-r-00000
                                        ┌────────────────────────────────┘
                                        ▼  POST /api/weather/pipeline/import
                         PostgreSQL weather_monthly_summary
                                        │  GET /api/weather/pipeline/verify
                                        ▼
                   compared with SQL GROUP BY ROLLUP over weather_daily → all periods match
```

Run everything with one command (EcoNet backend must be running):
```powershell
.\run_pipeline.ps1               # MapReduce via the local simulator
.\run_pipeline.ps1 -Mode hadoop  # MapReduce on HDFS (start-dfs.cmd first)
```

---

## 📂 Project Structure

```text
weather-analytics/
├── data/
│   ├── weather_data.dat         # 183 real daily records (exported from PostgreSQL)
│   └── fetch_open_meteo.py      # Standalone downloader (no database needed)
├── src/
│   ├── WeatherMapper.java       # Emits partial sums per month + OVERALL
│   ├── WeatherCombiner.java     # Pre-aggregates on the mapper side
│   ├── WeatherReducer.java      # Final averages + circular mean wind direction
│   ├── WeatherDriver.java       # Job configuration (ToolRunner)
│   ├── WeatherAggregate.java    # Shared sum/average arithmetic
│   ├── WeatherLocalSimulator.java  # Same logic without Hadoop
│   ├── WeatherPredictor.java    # 3-month forecast (Java)
│   └── ml_predict.py            # 3-month forecast (Python)
├── mapreduce_output/
│   ├── part-r-00000             # MapReduce output (imported into PostgreSQL)
│   └── report.txt               # Human-readable table
├── predicted_weather_next_3_months.csv
├── predictions_summary.txt
└── run_pipeline.ps1             # End-to-end pipeline script
```

---

## 🚀 Running the MapReduce job

### Option A: Local simulator (no Hadoop needed)
```powershell
javac -d build src/WeatherAggregate.java src/WeatherLocalSimulator.java
java -cp build WeatherLocalSimulator data/weather_data.dat mapreduce_output
```

### Option B: Apache Hadoop (HDFS)
1. **Compile and package** (`--release 8` keeps it compatible with Hadoop's Java 8/11 runtime):
   ```bash
   javac --release 8 -classpath "$(hadoop classpath)" -d build src/WeatherAggregate.java src/WeatherMapper.java src/WeatherCombiner.java src/WeatherReducer.java src/WeatherDriver.java
   jar -cvf weather.jar -C build/ .
   ```
2. **Upload the dataset to HDFS:**
   ```bash
   hdfs dfs -mkdir -p /weather_input
   hdfs dfs -put -f data/weather_data.dat /weather_input/
   ```
3. **Run the job:**
   ```bash
   hadoop jar weather.jar WeatherDriver /weather_input/weather_data.dat /weather_output
   ```
4. **View the output:**
   ```bash
   hdfs dfs -cat /weather_output/part-r-00000
   ```

### Output format
```text
MONTH_2026-04	days=30,temp=31.64,rainfall=0.03,humidity=55.20,pressure=1008.52,wind_speed=9.68,wind_dir=144.23
```

### Design notes
- **Combiner**: the mapper emits sums (`count, temp, rainfall, …`) instead of raw values. Sums can be added in
  any order, so a Combiner can merge them on each mapper node. Only one record per key per mapper is shuffled,
  instead of one per day. (Averages could not be combined like this: an average of averages is wrong.)
- **Circular mean for wind direction**: a plain average of 350° and 10° gives 180° (south), but both winds are
  from the north. Each direction is turned into a unit vector `(sin θ, cos θ)`, the vectors are summed, and the
  mean direction is `atan2(Σsin, Σcos)`.

---

## 📊 MapReduce Output: real Vellore data (Apr–Sep 2026)

| Period | Days | Avg Temp (°C) | Avg Rainfall (mm/day) | Avg Humidity (%) | Avg Pressure (hPa) | Avg Wind Speed (km/h) | Mean Wind Dir (°) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **2026-04** | 30 | 31.64 | 0.03 | 55.20 | 1008.52 | 9.68 | 144.23 |
| **2026-05** | 31 | 30.95 | 0.99 | 59.39 | 1006.55 | 9.86 | 215.96 |
| **2026-06** | 30 | 30.33 | 2.36 | 60.30 | 1006.94 | 12.67 | 268.14 |
| **2026-07** | 31 | 30.94 | 1.54 | 54.00 | 1006.18 | 14.47 | 268.53 |
| **2026-08** | 31 | 29.54 | 4.09 | 63.65 | 1007.39 | 12.86 | 271.97 |
| **2026-09** | 30 | 29.13 | 4.31 | 68.27 | 1009.16 | 9.59 | 256.69 |
| **OVERALL** | **183** | **30.42** | **2.22** | **60.11** | **1007.44** | **11.54** | **252.92** |

What the data shows: April is hot and almost dry. Winds swing from south-easterly in April to steady
westerlies (~270°) from June with the southwest monsoon. Rainfall builds through August–September, ahead of
the northeast monsoon (Oct–Dec), which is a major rain season for interior Tamil Nadu.

These numbers match the SQL `GROUP BY ROLLUP` over `weather_daily` exactly (checked by
`GET /api/weather/pipeline/verify`) and an independent Python calculation.

---

## 🤖 3-Month Forecast (Oct–Dec 2026)

`WeatherPredictor.java` and `ml_predict.py` start from the September average and apply fixed seasonal trends:

| Month | Days | Pred Temp (°C) | Pred Rainfall (mm) | Pred Humidity (%) | Pred Pressure (hPa) | Pred Wind Speed (km/h) | Pred Wind Dir (°) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **2026-10** | 31 | 27.76 | 1.91 | 64.00 | 1010.26 | 9.18 | 272.64 |
| **2026-11** | 30 | 24.91 | 0.30 | 56.96 | 1012.68 | 8.18 | 312.42 |
| **2026-12** | 31 | 22.16 | 0.12 | 49.42 | 1014.71 | 6.50 | 236.07 |

> ⚠️ **Known limitation:** the trends are hard-coded, not learned from data. They assume rain stops after
> September, but October–December is the northeast monsoon season in Vellore. This forecast is due to be
> replaced by a model trained on multi-year Open-Meteo history and stored in the `weather_forecast` table.
