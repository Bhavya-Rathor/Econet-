import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * WeatherMapper:
 * Reads daily weather data line-by-line from a .dat file.
 * Emits key-value pairs where:
 *   Key   = Month (e.g., "MONTH_2026-04") and "OVERALL_6_MONTHS"
 *   Value = "temp,rainfall,humidity,pressure,windSpeed,windDir"
 */
public class WeatherMapper extends Mapper<LongWritable, Text, Text, Text> {

    private Text outKeyMonth = new Text();
    private Text outKeyOverall = new Text("OVERALL_6_MONTHS");
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
            return;
        }

        try {
            String date = tokens[0].trim();
            double temp = Double.parseDouble(tokens[1].trim());
            double rainfall = Double.parseDouble(tokens[2].trim());
            double humidity = Double.parseDouble(tokens[3].trim());
            double pressure = Double.parseDouble(tokens[4].trim());
            double windSpeed = Double.parseDouble(tokens[5].trim());
            double windDir = Double.parseDouble(tokens[6].trim());

            // Combine attributes into a comma-separated string
            String valStr = String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                    temp, rainfall, humidity, pressure, windSpeed, windDir);
            outValue.set(valStr);

            // Emit Monthly key
            if (date.length() >= 7) {
                outKeyMonth.set("MONTH_" + date.substring(0, 7));
                context.write(outKeyMonth, outValue);
            }

            // Emit Overall 6-Month key
            context.write(outKeyOverall, outValue);

        } catch (NumberFormatException ignored) {
            // Skip invalid rows
        }
    }
}
