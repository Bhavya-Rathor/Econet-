# EcoNet + Weather Analytics

Cloud Computing & DBMS project combining two parts:

| Folder | What it is |
|---|---|
| [`econet-api/`](econet-api) | **EcoNet**: Spring Boot 3 + PostgreSQL backend. Ingests live Open-Meteo telemetry for Vellore, evaluates threshold rules, raises incident alerts; Docker / cloud deployment. Also hosts the weather pipeline API and tables. |
| [`weather-analytics/`](weather-analytics) | **Weather Analytics**: Hadoop MapReduce job (monthly & overall averages of temperature, rainfall, humidity, pressure, wind) and a 3-month forecast. |

## How they connect

```text
Open-Meteo archive → PostgreSQL weather_daily → export .dat → HDFS → Hadoop MapReduce
      → part-r-00000 → PostgreSQL weather_monthly_summary → verified against SQL GROUP BY ROLLUP
```

| Endpoint | Purpose |
|---|---|
| `POST /api/weather/pipeline/backfill?start=&end=` | Load real daily weather from Open-Meteo into `weather_daily` |
| `GET  /api/weather/pipeline/export?start=&end=` | Download `weather_daily` as the MapReduce input `.dat` |
| `POST /api/weather/pipeline/import` (text/plain) | Load `part-r-00000` into `weather_monthly_summary` |
| `GET  /api/weather/pipeline/summary` | Stored MapReduce results |
| `GET  /api/weather/pipeline/verify?start=&end=` | Compare MapReduce results with the SQL aggregation |

New tables (see [`econet-api/schema.sql`](econet-api/schema.sql)): `weather_daily`, `weather_monthly_summary`,
`weather_forecast`, plus the view `v_weather_monthly_sql`.

## Running

```bash
cd econet-api
cp .env.example .env   # then set your own passwords
docker compose up --build
```

Then, in another terminal:

```powershell
cd weather-analytics
.\run_pipeline.ps1
```
