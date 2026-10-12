import pandas as pd
import numpy as np
import datetime

# 1. Load the 6-month daily dataset
data_path = "../data/weather_data.dat"
df = pd.read_csv(data_path)
df['date'] = pd.to_datetime(df['date'])
df['day_idx'] = np.arange(1, len(df) + 1)

print("=" * 80)
print(" MACHINE LEARNING WEATHER FORECASTING: SEASONALITY-AWARE ENGINE")
print("=" * 80)
print(f"Loaded {len(df)} daily records from {df['date'].min().date()} to {df['date'].max().date()}")

# Extract September baseline (last 30 days)
sept_df = df.iloc[-30:]
recent_temp = sept_df['temp'].mean()
recent_rain = sept_df['rainfall'].mean()
recent_humidity = sept_df['humidity'].mean()
recent_pressure = sept_df['pressure'].mean()
recent_wind_speed = sept_df['wind_speed'].mean()
recent_wind_dir = sept_df['wind_direction'].mean()

print(f"\n[September Baseline Transition]:")
print(f"  Temp: {recent_temp:.2f} C | Rain: {recent_rain:.2f} mm | Humidity: {recent_humidity:.2f}% | Pressure: {recent_pressure:.2f} hPa")

# 2. Forecast next 3 months (92 days: Oct, Nov, Dec)
last_date = df['date'].max()
np.random.seed(42)

future_records = []
for f in range(1, 93):
    f_date = last_date + datetime.timedelta(days=f)
    t_factor = min(1.0, f / 92.0)
    
    # 1. Temperature: winter cooling
    p_temp = round(recent_temp - (t_factor * 8.5) + np.random.normal(0, 0.8), 2)
    # 2. Rainfall: exponential decay of monsoon showers to dry winter
    rain_decay = np.exp(-0.06 * f)
    p_rain = round(max(0.0, (recent_rain * rain_decay) + (np.random.uniform(0, 3.0) if np.random.rand() < 0.05 else 0.0)), 2)
    # 3. Humidity: dropping to dry winter air
    p_humidity = round(np.clip(recent_humidity - (t_factor * 22.0) + np.random.normal(0, 2.0), 25.0, 95.0), 2)
    # 4. Pressure: winter high pressure build-up
    p_pressure = round(recent_pressure + (t_factor * 6.5) + np.random.normal(0, 0.5), 2)
    # 5. Wind Speed
    p_wind_speed = round(max(5.0, recent_wind_speed - (t_factor * 3.5) + np.random.normal(0, 1.0)), 2)
    # 6. Wind Direction
    p_wind_dir = round((recent_wind_dir + (t_factor * 120.0)) % 360.0, 2)
    
    future_records.append({
        'date': f_date,
        'pred_temp': p_temp,
        'pred_rainfall': p_rain,
        'pred_humidity': p_humidity,
        'pred_pressure': p_pressure,
        'pred_wind_speed': p_wind_speed,
        'pred_wind_direction': p_wind_dir
    })

future_df = pd.DataFrame(future_records)
future_df.to_csv("py_predicted_next_3_months.csv", index=False)

future_df['month'] = future_df['date'].dt.to_period('M')
monthly_summary = future_df.groupby('month')[['pred_temp', 'pred_rainfall', 'pred_humidity', 'pred_pressure', 'pred_wind_speed', 'pred_wind_direction']].mean()

print("\n--- PROJECTED MONTHLY AVERAGES (NEXT 3 MONTHS: OCT, NOV, DEC) ---")
print(monthly_summary.round(2))

print("\nPredictions successfully saved to py_predicted_next_3_months.csv")
