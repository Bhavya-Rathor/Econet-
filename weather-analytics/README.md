# Weather Analytics: Hadoop MapReduce & Machine Learning Forecasting

This project fulfills the assignment requirements:
1. **6-Month Daily Weather Dataset (`.dat` file):** Contains attributes `date, temp, rainfall, humidity, pressure, wind_speed, wind_direction`.
2. **Hadoop MapReduce (Java):** Calculates the average temperature, rainfall, and all weather attributes across each month and over the entire 6-month period, writing results to an output file.
3. **Seasonality-Aware Machine Learning Model:** Forecasts all weather attributes for the **next 3 months** (92 days: October, November, December) accounting for the transition from monsoon to winter.

---

## 📂 Project Structure

```text
WeatherAnalytics/
├── data/
│   ├── weather_data.dat            # 6-month daily weather dataset (183 records)
│   └── generate_data.py            # Python generator for realistic dataset
├── src/
│   ├── WeatherJob.java             # All-in-one Hadoop MapReduce (Mapper, Reducer, Driver)
│   ├── WeatherMapper.java          # Standalone Mapper class
│   ├── WeatherReducer.java         # Standalone Reducer class
│   ├── WeatherDriver.java          # Standalone Driver class
│   ├── WeatherLocalSimulator.java  # Local Java runner (simulates MapReduce without HDFS)
│   ├── WeatherPredictor.java       # Java ML predictor (Seasonality & Time-Series Regression)
│   └── ml_predict.py               # Python ML predictor
├── build/                          # Compiled .class files
├── weather.jar                     # Packaged Hadoop JAR
├── mapreduce_output.txt            # MapReduce output file with 6-month averages
├── predicted_weather_next_3_months.csv  # Daily ML predictions for the next 3 months
└── predictions_summary.txt         # Summary of ML monthly forecasts and metrics
```

---

## 🚀 Part 1: Running the Hadoop MapReduce Program

### Option A: Local Run (No HDFS cluster needed)
If you want to test and verify the MapReduce logic locally without starting Hadoop namenode/datanode daemons:
```powershell
javac -d build src/WeatherLocalSimulator.java
java -cp build WeatherLocalSimulator data/weather_data.dat mapreduce_output.txt
```

### Option B: Running on an Apache Hadoop Cluster (HDFS)
1. **Compile with Hadoop classpath and package JAR:**
   ```bash
   javac -classpath $(hadoop classpath) -d build src/WeatherMapper.java src/WeatherReducer.java src/WeatherDriver.java
   jar -cvf weather.jar -C build/ .
   ```
2. **Upload `.dat` file to HDFS:**
   ```bash
   hdfs dfs -mkdir -p /weather_input
   hdfs dfs -put data/weather_data.dat /weather_input/
   ```
3. **Run the Hadoop Job:**
   ```bash
   hadoop jar weather.jar WeatherDriver /weather_input/weather_data.dat /weather_output
   ```
4. **View MapReduce Output:**
   ```bash
   hdfs dfs -cat /weather_output/part-r-00000
   ```

---

## 📊 MapReduce Output (`mapreduce_output.txt`)

Notice how the monthly breakdown captures the summer-to-monsoon fluctuations:

| Group Key | Days | Avg Temp (°C) | Avg Rainfall (mm) | Avg Humidity (%) | Avg Pressure (hPa) | Avg Wind Speed (km/h) | Avg Wind Dir (deg) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **MONTH_2026-04** | 30 | 33.05 C | 1.49 mm | 53.33% | 1010.12 hPa | 13.55 km/h | 167.85 deg |
| **MONTH_2026-05** | 31 | 33.22 C | 1.55 mm | 51.29% | 1009.68 hPa | 15.56 km/h | 173.03 deg |
| **MONTH_2026-06** | 30 | 28.48 C | 23.18 mm | 80.97% | 1004.33 hPa | 21.18 km/h | 222.94 deg |
| **MONTH_2026-07** | 31 | 28.17 C | 21.13 mm | 84.71% | 1005.79 hPa | 21.14 km/h | 233.04 deg |
| **MONTH_2026-08** | 31 | 28.64 C | 21.26 mm | 83.69% | 1005.47 hPa | 19.75 km/h | 228.42 deg |
| **MONTH_2026-09** | 30 | 30.12 C | 5.29 mm | 69.00% | 1009.23 hPa | 15.75 km/h | 169.24 deg |
| **OVERALL_6_MONTHS** | **183** | **30.28 C** | **12.35 mm** | **70.54%** | **1007.43 hPa** | **17.84 km/h** | **199.29 deg** |

---

## 🤖 Part 2: Seasonality-Aware ML Forecasting (Next 3 Months)

The ML forecasting model explicitly takes into account the **seasonal shift into winter**:
1. **Temperature:** drops progressively from ~28.75 °C (October) to ~23.15 °C (December).
2. **Rainfall:** decays from 2.34 mm down to ~0.13 mm as monsoon rains conclude.
3. **Humidity:** decreases from 64.73% to 50.15% (dry winter air).
4. **Pressure:** rises to 1014.78 hPa (cold dense air creates winter high pressure).

### 📈 ML Forecast Results (Next 3 Months: Oct, Nov, Dec):
| Forecast Month | Days | Pred Temp (°C) | Pred Rainfall (mm) | Pred Humidity (%) | Pred Pressure (hPa) | Pred Wind Speed (km/h) | Pred Wind Dir (deg) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **MONTH_2026-10** | 31 | 28.75 C | 2.34 mm | 64.73% | 1010.33 hPa | 15.34 km/h | 190.11 deg |
| **MONTH_2026-11** | 30 | 25.91 C | 0.37 mm | 57.70% | 1012.75 hPa | 14.33 km/h | 229.90 deg |
| **MONTH_2026-12** | 31 | 23.15 C | 0.13 mm | 50.15% | 1014.78 hPa | 12.64 km/h | 269.68 deg |
| **OVERALL_NEXT_3_M**| **92** | **25.94 C** | **0.95 mm** | **57.52%** | **1012.62 hPa** | **14.10 km/h** | **229.90 deg** |
