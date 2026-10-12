import datetime
import random
import os

# Generate 6 months of daily weather data (April 1, 2026 to September 30, 2026 - 183 days)
start_date = datetime.date(2026, 4, 1)
total_days = 183

random.seed(42)  # For reproducibility

output_file = os.path.join(os.path.dirname(__file__), "weather_data.dat")

lines = ["date,temp,rainfall,humidity,pressure,wind_speed,wind_direction\n"]

base_temp = 30.0
base_pressure = 1012.0
base_humidity = 60.0

for day_idx in range(total_days):
    curr_date = start_date + datetime.timedelta(days=day_idx)
    date_str = curr_date.strftime("%Y-%m-%d")
    
    # Seasonal progression (summer warming then monsoon cooling)
    # Months 4-5 (April-May): hot, low rainfall
    # Months 6-8 (June-August): monsoon, higher rain, higher humidity
    # Month 9 (September): retreating monsoon
    month = curr_date.month
    
    if month in (4, 5):  # Pre-monsoon / Summer
        temp = round(32.0 + random.uniform(-3.5, 6.0), 2)
        rainfall = round(random.choices([0.0, random.uniform(1.0, 15.0)], weights=[0.85, 0.15])[0], 2)
        humidity = round(50.0 + random.uniform(-10.0, 15.0), 2)
        pressure = round(1010.0 + random.uniform(-3.0, 3.0), 2)
        wind_speed = round(12.0 + random.uniform(-4.0, 8.0), 2)
        wind_dir = round(random.uniform(90.0, 240.0), 1)
    elif month in (6, 7, 8):  # Monsoon
        temp = round(28.0 + random.uniform(-3.0, 4.0), 2)
        rainfall = round(random.choices([0.0, random.uniform(5.0, 50.0)], weights=[0.30, 0.70])[0], 2)
        humidity = round(78.0 + random.uniform(-8.0, 18.0), 2)
        humidity = min(humidity, 98.0)
        pressure = round(1005.0 + random.uniform(-4.0, 4.0), 2)
        wind_speed = round(18.0 + random.uniform(-5.0, 12.0), 2)
        wind_dir = round(random.uniform(180.0, 270.0), 1)
    else:  # September - Transition
        temp = round(29.5 + random.uniform(-2.5, 4.0), 2)
        rainfall = round(random.choices([0.0, random.uniform(2.0, 25.0)], weights=[0.60, 0.40])[0], 2)
        humidity = round(68.0 + random.uniform(-10.0, 12.0), 2)
        pressure = round(1009.0 + random.uniform(-3.0, 3.0), 2)
        wind_speed = round(14.0 + random.uniform(-3.0, 7.0), 2)
        wind_dir = round(random.uniform(120.0, 220.0), 1)
        
    lines.append(f"{date_str},{temp},{rainfall},{humidity},{pressure},{wind_speed},{wind_dir}\n")

with open(output_file, "w") as f:
    f.writelines(lines)

print(f"Generated {len(lines)-1} daily weather records in {output_file}")
