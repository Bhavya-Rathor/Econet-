# EcoNet + Weather Analytics

Cloud Computing & DBMS project combining two parts:

| Folder | What it is |
|---|---|
| [`econet-api/`](econet-api) | **EcoNet**: Spring Boot 3 + PostgreSQL backend. Ingests live Open-Meteo telemetry for Vellore, evaluates threshold rules, raises incident alerts; Docker / cloud deployment. |
| [`weather-analytics/`](weather-analytics) | **Weather Analytics**: Hadoop MapReduce job (monthly & 6-month averages of temperature, rainfall, humidity, pressure, wind) and a 3-month seasonal forecast. |

## Running EcoNet locally
```bash
cd econet-api
cp .env.example .env   # then set your own passwords
docker compose up --build
```

## Planned integration
Open-Meteo → PostgreSQL (`weather_daily`) → export to HDFS → Hadoop MapReduce → results back into PostgreSQL → Spring Boot REST API & dashboard.
