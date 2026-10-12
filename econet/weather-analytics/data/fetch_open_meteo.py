"""
Downloads REAL daily weather observations for Vellore (EcoNet monitoring zone)
from the Open-Meteo Historical Weather API (ERA5 reanalysis) and writes them
in the .dat format consumed by the Hadoop MapReduce job.

Usage:
    python fetch_open_meteo.py [start_date] [end_date] [output_file]
    python fetch_open_meteo.py 2026-04-01 2026-09-30 weather_data.dat

No third-party packages required (standard library only).
"""
import json
import sys
import urllib.parse
import urllib.request

LATITUDE = 12.9165   # Vellore, Tamil Nadu (same coordinates as EcoNet's MonitoringZone)
LONGITUDE = 79.1325

# Open-Meteo daily variable -> .dat column
VARIABLES = [
    ("temperature_2m_mean", "temp"),             # deg C
    ("precipitation_sum", "rainfall"),           # mm
    ("relative_humidity_2m_mean", "humidity"),   # %
    ("pressure_msl_mean", "pressure"),           # hPa (mean sea level)
    ("wind_speed_10m_mean", "wind_speed"),       # km/h
    ("wind_direction_10m_dominant", "wind_direction"),  # degrees
]


def fetch(start_date, end_date):
    params = {
        "latitude": LATITUDE,
        "longitude": LONGITUDE,
        "start_date": start_date,
        "end_date": end_date,
        "daily": ",".join(v for v, _ in VARIABLES),
        "timezone": "Asia/Kolkata",
    }
    url = "https://archive-api.open-meteo.com/v1/archive?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=30) as resp:
        return json.load(resp)["daily"]


def main():
    start_date = sys.argv[1] if len(sys.argv) > 1 else "2026-04-01"
    end_date = sys.argv[2] if len(sys.argv) > 2 else "2026-09-30"
    output_file = sys.argv[3] if len(sys.argv) > 3 else "weather_data.dat"

    daily = fetch(start_date, end_date)
    rows, skipped = [], 0
    for i, date in enumerate(daily["time"]):
        values = [daily[v][i] for v, _ in VARIABLES]
        if any(x is None for x in values):
            skipped += 1  # Open-Meteo returns null for days not yet published
            continue
        rows.append(date + "," + ",".join(str(x) for x in values))

    with open(output_file, "w", newline="\n") as f:
        f.write("date," + ",".join(col for _, col in VARIABLES) + "\n")
        f.write("\n".join(rows) + "\n")

    print(f"Wrote {len(rows)} real daily records for Vellore ({start_date} to {end_date}) -> {output_file}")
    if skipped:
        print(f"Skipped {skipped} days with missing values")


if __name__ == "__main__":
    main()
