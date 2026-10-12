import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * WeatherMapper:
 * Reads daily weather data line-by-line from a .dat file
 * (date,temp,rainfall,humidity,pressure,wind_speed,wind_direction).
 * Emits key-value pairs where:
 *   Key   = Month (e.g., "MONTH_2026-04") and "OVERALL"
 *   Value = partial sums "count,temp,rainfall,humidity,pressure,windSpeed,sin(windDir),cos(windDir)"
 *
 * Emitting sums (with count = 1) instead of raw values lets a Combiner pre-aggregate
 * on each mapper node, and wind direction is carried as a unit vector so the
 * Reducer can compute a correct circular mean (e.g. mean of 350 deg and 10 deg = 0 deg).
 */
public class WeatherMapper extends Mapper<LongWritable, Text, Text, Text> {

    private Text outKeyMonth = new Text();
    private Text outKeyOverall = new Text("OVERALL");
    private Text outValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();

        // Skip empty lines and CSV header
        if (line.isEmpty() || line.toLowerCase().startsWith("date")) {
            return;
        }

        String[] tokens = line.split(",");
        if (tokens.length < 7) {
            context.getCounter("WeatherData", "MALFORMED_ROWS").increment(1);
            return;
        }

        try {
            String date = tokens[0].trim();
            double temp = Double.parseDouble(tokens[1].trim());
            double rainfall = Double.parseDouble(tokens[2].trim());
            double humidity = Double.parseDouble(tokens[3].trim());
            double pressure = Double.parseDouble(tokens[4].trim());
            double windSpeed = Double.parseDouble(tokens[5].trim());
            double windRad = Math.toRadians(Double.parseDouble(tokens[6].trim()));

            outValue.set(WeatherAggregate.format(new double[]{
                    1, temp, rainfall, humidity, pressure, windSpeed, Math.sin(windRad), Math.cos(windRad)
            }));

            // Emit Monthly key
            if (date.length() >= 7) {
                outKeyMonth.set("MONTH_" + date.substring(0, 7));
                context.write(outKeyMonth, outValue);
            }

            // Emit Overall key
            context.write(outKeyOverall, outValue);
            context.getCounter("WeatherData", "VALID_DAYS").increment(1);

        } catch (NumberFormatException ignored) {
            context.getCounter("WeatherData", "MALFORMED_ROWS").increment(1);
        }
    }
}
