/**
 * WeatherAggregate:
 * Shared arithmetic for the MapReduce job and the local simulator.
 *
 * A partial aggregate is a vector of 8 sums:
 *   [count, temp, rainfall, humidity, pressure, windSpeed, sin(windDir), cos(windDir)]
 * Sums can be added in any order (associative + commutative), which is what makes
 * the Combiner safe to use.
 *
 * Final output line format (value written by the Reducer, parsed by EcoNet's importer):
 *   days=30,temp=28.12,rainfall=1.49,humidity=53.33,pressure=1010.12,wind_speed=13.55,wind_dir=167.85
 */
public final class WeatherAggregate {

    public static final int FIELDS = 8;

    private WeatherAggregate() {}

    public static double[] parse(String csv) {
        String[] parts = csv.split(",");
        if (parts.length != FIELDS) {
            throw new IllegalArgumentException("Expected " + FIELDS + " fields but got " + parts.length + ": " + csv);
        }
        double[] v = new double[FIELDS];
        for (int i = 0; i < FIELDS; i++) {
            v[i] = Double.parseDouble(parts[i]);
        }
        return v;
    }

    public static String format(double[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < FIELDS; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.toString();
    }

    public static void addInto(double[] acc, double[] v) {
        for (int i = 0; i < FIELDS; i++) {
            acc[i] += v[i];
        }
    }

    /** Circular mean of wind direction in degrees, normalised to [0, 360). */
    public static double meanWindDirection(double sumSin, double sumCos) {
        double deg = Math.toDegrees(Math.atan2(sumSin, sumCos));
        return (deg + 360.0) % 360.0;
    }

    /** Converts summed vector into the final averages line. */
    public static String toAverages(double[] s) {
        double n = s[0];
        return String.format(java.util.Locale.ROOT,
                "days=%d,temp=%.2f,rainfall=%.2f,humidity=%.2f,pressure=%.2f,wind_speed=%.2f,wind_dir=%.2f",
                (long) n, s[1] / n, s[2] / n, s[3] / n, s[4] / n, s[5] / n, meanWindDirection(s[6], s[7]));
    }
}
