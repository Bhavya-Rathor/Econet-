"""
Seasonal harmonic regression forecast for Vellore weather.

Learns the yearly weather cycle from multi-year Open-Meteo history and predicts
daily weather for a future period. For every attribute it fits, by least squares:

    value(day) = a + b*t + sum_{k=1..3} [ c_k*sin(2*pi*k*doy/365.25) + d_k*cos(2*pi*k*doy/365.25) ]

  t   = years since the start of the history (captures long-term trend, e.g. warming)
  doy = day of year (the sin/cos pairs capture the seasonal cycle; k=2,3 let it
        have sharper shapes such as the Oct-Nov northeast-monsoon rain peak)

Wind direction is an angle, so its sin and cos are fitted separately and recombined
with atan2 (same idea as the circular mean in the MapReduce reducer).

Before forecasting, the script back-tests honestly: it trains only on data up to
the day before a past test window, predicts that window, and compares the error (MAE)
with the old method (last 30-day average carried forward).

Usage (from the weather-analytics folder):
    python data/fetch_open_meteo.py 2016-01-01 2026-09-30 data/history.dat
    python src/train_forecast.py
    python src/train_forecast.py data/history.dat 2026-10-01 2026-12-31
"""
import sys

import numpy as np
import pandas as pd

MODEL_NAME = "SEASONAL_HARMONIC_V1"
HARMONICS = 3
LINEAR_TARGETS = ["temp", "rainfall", "humidity", "pressure", "wind_speed"]
# Physical limits applied to predictions
CLIP = {"rainfall": (0, None), "humidity": (0, 100), "wind_speed": (0, None)}


def design_matrix(dates, origin):
    """Feature matrix: [1, t, sin(k w), cos(k w) for k = 1..HARMONICS]."""
    t = (dates - origin).dt.days.to_numpy() / 365.25
    w = 2 * np.pi * dates.dt.dayofyear.to_numpy() / 365.25
    cols = [np.ones_like(t), t]
    for k in range(1, HARMONICS + 1):
        cols += [np.sin(k * w), np.cos(k * w)]
    return np.column_stack(cols)


def fit_and_predict(train, future_dates):
    """Fit one least-squares model per attribute on `train`, predict for `future_dates`."""
    origin = train["date"].min()
    X_train = design_matrix(train["date"], origin)
    X_future = design_matrix(future_dates, origin)

    out = pd.DataFrame({"date": future_dates.reset_index(drop=True)})
    for col in LINEAR_TARGETS:
        coef, *_ = np.linalg.lstsq(X_train, train[col].to_numpy(), rcond=None)
        lo, hi = CLIP.get(col, (None, None))
        out[col] = np.clip(X_future @ coef, lo, hi)

    rad = np.radians(train["wind_direction"].to_numpy())
    coef_s, *_ = np.linalg.lstsq(X_train, np.sin(rad), rcond=None)
    coef_c, *_ = np.linalg.lstsq(X_train, np.cos(rad), rcond=None)
    out["wind_direction"] = np.degrees(np.arctan2(X_future @ coef_s, X_future @ coef_c)) % 360
    return out


def persistence_baseline(train, future_dates):
    """The old approach: average of the last 30 days, carried forward unchanged."""
    last = train.tail(30)
    out = pd.DataFrame({"date": future_dates.reset_index(drop=True)})
    for col in LINEAR_TARGETS:
        out[col] = last[col].mean()
    rad = np.radians(last["wind_direction"])
    out["wind_direction"] = np.degrees(np.arctan2(np.sin(rad).mean(), np.cos(rad).mean())) % 360
    return out


def mae(pred, actual, col):
    err = (pred[col].to_numpy() - actual[col].to_numpy())
    if col == "wind_direction":  # shortest way round the circle: 350 vs 10 is 20 deg apart
        err = (err + 180) % 360 - 180
    return np.abs(err).mean()


def monthly_means(df):
    m = df.groupby(df["date"].dt.to_period("M"))[LINEAR_TARGETS].mean()
    m["days"] = df.groupby(df["date"].dt.to_period("M")).size()
    return m


def run_window(history, test_start, test_end):
    """Train on everything before test_start, predict the window; return (actual, model, baseline)."""
    train = history[history["date"] < test_start]
    actual = history[(history["date"] >= test_start) & (history["date"] <= test_end)].reset_index(drop=True)
    return actual, fit_and_predict(train, actual["date"]), persistence_baseline(train, actual["date"])


def monthly_mae(pred, actual, col):
    return (monthly_means(pred)[col] - monthly_means(actual)[col]).abs().mean()


def backtest(history, test_start, test_end, years=5):
    """Rolling-origin back-test: repeat the same season for the last `years` years,
    each time training only on data before that season starts."""
    windows = []
    for y in range(years - 1, -1, -1):
        s = (pd.Timestamp(test_start) - pd.DateOffset(years=y)).strftime("%Y-%m-%d")
        e = (pd.Timestamp(test_end) - pd.DateOffset(years=y)).strftime("%Y-%m-%d")
        windows.append((s, e, *run_window(history, s, e)))

    lines = [f"BACK-TEST over {years} seasons ({windows[0][0]} .. {windows[-1][1]}),",
             "each predicted using only data from before that season started.",
             "",
             f"{'attribute':<16}{'daily MAE':>22}{'monthly-average MAE':>28}",
             f"{'':<16}{'old':>8}{'new':>8}{'better':>8}{'old':>11}{'new':>8}{'better':>9}"]
    for col in LINEAR_TARGETS + ["wind_direction"]:
        b = np.mean([mae(base, act, col) for _, _, act, _, base in windows])
        m = np.mean([mae(mod, act, col) for _, _, act, mod, _ in windows])
        if col == "wind_direction":
            lines.append(f"{col:<16}{b:>8.2f}{m:>8.2f}{(1 - m / b) * 100:>7.0f}%")
            continue
        bm = np.mean([monthly_mae(base, act, col) for _, _, act, _, base in windows])
        mm = np.mean([monthly_mae(mod, act, col) for _, _, act, mod, _ in windows])
        lines.append(f"{col:<16}{b:>8.2f}{m:>8.2f}{(1 - m / b) * 100:>7.0f}%{bm:>11.2f}{mm:>8.2f}{(1 - mm / bm) * 100:>8.0f}%")

    _, _, actual, model, base = windows[-1]
    lines += ["", f"Most recent season ({windows[-1][0]} .. {windows[-1][1]}): actual / new model / old method"]
    act_m, mod_m, base_m = monthly_means(actual), monthly_means(model), monthly_means(base)
    for period in act_m.index:
        lines.append(f"  {period}  temp {act_m.loc[period, 'temp']:5.2f} / {mod_m.loc[period, 'temp']:5.2f} / {base_m.loc[period, 'temp']:5.2f} C"
                     f"   rain {act_m.loc[period, 'rainfall']:5.2f} / {mod_m.loc[period, 'rainfall']:5.2f} / {base_m.loc[period, 'rainfall']:5.2f} mm/day"
                     f"   humidity {act_m.loc[period, 'humidity']:5.1f} / {mod_m.loc[period, 'humidity']:5.1f} / {base_m.loc[period, 'humidity']:5.1f} %")
    return lines


def main():
    history_file = sys.argv[1] if len(sys.argv) > 1 else "data/history.dat"
    start = sys.argv[2] if len(sys.argv) > 2 else "2026-10-01"
    end = sys.argv[3] if len(sys.argv) > 3 else "2026-12-31"

    history = pd.read_csv(history_file, parse_dates=["date"]).sort_values("date").reset_index(drop=True)
    print(f"Loaded {len(history)} days: {history['date'].min().date()} .. {history['date'].max().date()}")

    # 1. Honest evaluation on the same season one year earlier
    test_start = (pd.Timestamp(start) - pd.DateOffset(years=1)).strftime("%Y-%m-%d")
    test_end = (pd.Timestamp(end) - pd.DateOffset(years=1)).strftime("%Y-%m-%d")
    report = backtest(history, test_start, test_end)

    # 2. Retrain on everything and forecast the real future period
    future_dates = pd.Series(pd.date_range(start, end, freq="D"))
    forecast = fit_and_predict(history, future_dates)

    out_csv = "forecast_next_3_months.csv"
    forecast.assign(date=forecast["date"].dt.strftime("%Y-%m-%d")).round(2).to_csv(out_csv, index=False)

    fm = monthly_means(forecast)
    report += ["", f"FORECAST ({MODEL_NAME}): {start}..{end}, trained on all {len(history)} days",
               f"{'month':<10}{'days':>5}{'temp C':>9}{'rain mm':>9}{'humid %':>9}{'press hPa':>11}{'wind km/h':>11}"]
    for period, r in fm.iterrows():
        report.append(f"{str(period):<10}{int(r['days']):>5}{r['temp']:>9.2f}{r['rainfall']:>9.2f}"
                      f"{r['humidity']:>9.2f}{r['pressure']:>11.2f}{r['wind_speed']:>11.2f}")

    with open("forecast_evaluation.txt", "w", encoding="utf-8") as f:
        f.write("\n".join(report) + "\n")
    print("\n".join(report))
    print(f"\nWrote {out_csv} ({len(forecast)} days) and forecast_evaluation.txt")


if __name__ == "__main__":
    main()
